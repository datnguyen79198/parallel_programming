package ru.bmstu.anonymizer;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;

import java.io.IOException;
import java.util.concurrent.CompletionStage;

import ru.bmstu.anonymizer.SystemServer;
import ru.bmstu.anonymizer.Store.StoreActor;
import ru.bmstu.anonymizer.ZookeeperService;
import ru.bmstu.anonymizer.Messages.GetRandomServerMessage;

import org.apache.zookeeper.*;

/**
 * http://localhost:8080/? url=http://rambler.ru&count=20
 *
 */
public class WebServer {
    private static final String domain = "localhost";
    private static final int port = 8080;

    public static void main( String[] args ) throws IOException {
        System.out.println( "Start create server");

        ActorSystem system = ActorSystem.create("routes");

        private Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);

        SystemServer server = new SystemServer(system, http);

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow =
              server.createRoute().flow(system,materializer);

        final CompletionStage<ServerBinding> binding = http.bindAndHandle(
              routeFlow,
              ConnectHttp.toHost(domain,port),
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
