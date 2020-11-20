package ru.bmstu.AkkaPingApp;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.Route;

import static akka.http.javadsl.server.Directives.*;

import akka.pattern.PatternsCS;
import akka.routing.RoundRobinPool;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;

import java.io.IOException;
import java.util.concurrent.CompletionStage;

import org.asynchttpclient.AsyncHttpClient; //REST api
import org.asynchttpclient.Dsl; //init client

/**
 * http://localhost:8080/?testUrl=http://rambler.ru&count=20
 *
 */

private WebServer(final ActorSystem system) {
    storeActor = system.actorOf(Props.create(StoreActor.class));
}


public class WebServer {

    private static final String domain = "localhost";
    private static final int port = 8080;

    public static void main( String[] args ) {
      ActorSystem system = ActorSystem.create("routes");

      final Http http = Http.get(system);
      final ActorMaterializer materializer = ActorMaterializer.create(system);

      WebServer server = new WebServer(system);
      final Flow<HttpRequest, HttpResponse, NotUsed> httpFlow = getHttpFlow(materializer);

      final CompletionStage<ServerBinding> binding = http.bindAndHandle (
          httpFlow,
          ConnectHttp.toHost(domain, port),
          materializer
      );

      System.out.println("Server is started at http://localhost:8080");

      System.in.read();

      binding
            .thenCompose(ServerBinding::unbind)
            .thenAccept(unbound -> system.terminate());

      System.out.println("Server off");

    }
}
