package de.sonallux.spotify.graphql;

import de.sonallux.spotify.core.EndpointHelper;
import de.sonallux.spotify.core.SpotifyWebApiUtils;
import de.sonallux.spotify.core.model.SpotifyWebApi;
import de.sonallux.spotify.core.model.SpotifyWebApiObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class SpotifyWebApiAdjuster {

    public static void adjust(SpotifyWebApi spotifyWebApi) {
        EndpointHelper.fixDuplicateEndpointParameters(spotifyWebApi);
        EndpointHelper.splitEndpoints(spotifyWebApi);
        removeSimplifiedObjects(spotifyWebApi);
        expandPagingObjects(spotifyWebApi);
    }

    private static void removeSimplifiedObjects(SpotifyWebApi spotifyWebApi) {
        //Remove simplified objects
        spotifyWebApi.getObjects().keySet().stream()
            .filter(name -> name.contains("Simplified"))
            .collect(Collectors.toList())
            .forEach(spotifyWebApi.getObjects()::remove);

        //Replace references to simplified objects with references to full objects
        spotifyWebApi.getObjectList().stream()
            .flatMap(object -> object.getProperties().stream())
            .forEach(p -> p.setType(p.getType().replace("Simplified", "")));

        //Replace response type with simplified object to full objects
        spotifyWebApi.getCategoryList().stream()
            .flatMap(c -> c.getEndpointList().stream())
            .flatMap(e -> e.getResponseTypes().stream())
            .forEach(r -> r.setType(r.getType().replace("Simplified", "")));
    }

    private static void expandPagingObjects(SpotifyWebApi spotifyWebApi) {
        var pagingObject = spotifyWebApi.getObject("PagingObject").orElseThrow();
        var cursorPagingObject = spotifyWebApi.getObject("CursorPagingObject").orElseThrow();

        var newPagingObjects = new TreeMap<String, SpotifyWebApiObject>();

        spotifyWebApi.getObjectList().stream()
            .flatMap(o -> o.getProperties().stream())
            .forEach(p -> {
                var newType = getExpandedPagingObjectName(newPagingObjects, p.getType(), pagingObject, cursorPagingObject);
                if (newType != null) {
                    p.setType(newType);
                }
            });

        spotifyWebApi.getCategoryList().stream()
            .flatMap(c -> c.getEndpointList().stream())
            .flatMap(e -> e.getResponseTypes().stream())
            .forEach(r -> {
                var newType = getExpandedPagingObjectName(newPagingObjects, r.getType(), pagingObject, cursorPagingObject);
                if (newType != null) {
                    r.setType(newType);
                }
            });

        var objectsMap = spotifyWebApi.getObjects();
        objectsMap.remove(pagingObject.getName());
        objectsMap.remove(cursorPagingObject.getName());
        newPagingObjects.forEach((k, object) -> objectsMap.put(object.getName(), object));
    }

    private static String getExpandedPagingObjectName(Map<String, SpotifyWebApiObject> expandedObjects, String type, SpotifyWebApiObject pagingObject, SpotifyWebApiObject cursorPagingObject) {
        Matcher matcher;
        if ((matcher = SpotifyWebApiUtils.PAGING_OBJECT_TYPE_PATTERN.matcher(type)).matches()) {
            var itemType = matcher.group(1);
            return expandedObjects.computeIfAbsent(type, t -> expandPagingObject(pagingObject, itemType)).getName();
        } else if ((matcher = SpotifyWebApiUtils.CURSOR_PAGING_OBJECT_TYPE_PATTERN.matcher(type)).matches()) {
            var itemType = matcher.group(1);
            return expandedObjects.computeIfAbsent(type, t -> expandPagingObject(cursorPagingObject, itemType)).getName();
        } else {
            return null;
        }
    }

    private static SpotifyWebApiObject expandPagingObject(SpotifyWebApiObject basePagingObject, String itemType) {
        var newName = basePagingObject.getName().replace("Object", "") + itemType;
        var newObject = new SpotifyWebApiObject(newName, basePagingObject.getLink());
        for (var property : basePagingObject.getProperties()) {
            if (property.getName().equals("items")) {
                newObject.addProperty(new SpotifyWebApiObject.Property("items", "Array[" + itemType + "]", property.getDescription()));
            } else {
                newObject.addProperty(property);
            }
        }
        return newObject;
    }
}
