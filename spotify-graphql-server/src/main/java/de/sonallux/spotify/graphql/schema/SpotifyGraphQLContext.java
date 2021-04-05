package de.sonallux.spotify.graphql.schema;

import graphql.kickstart.servlet.context.DefaultGraphQLServletContext;
import org.dataloader.DataLoaderRegistry;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SpotifyGraphQLContext extends DefaultGraphQLServletContext {

    public SpotifyGraphQLContext(DataLoaderRegistry dataLoaderRegistry, Subject subject, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        super(dataLoaderRegistry, subject, httpServletRequest, httpServletResponse);
    }
}
