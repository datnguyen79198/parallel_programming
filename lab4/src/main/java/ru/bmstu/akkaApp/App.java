package ru.bmstu.akkaApp;

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

import ru.bmstu.akkaApp.Store.Message;
import ru.bmstu.akkaApp.Store.StoreActor;
import ru.bmstu.akkaApp.Test.TestActor;
import ru.bmstu.akkaApp.Test.TestPackageActor;
import ru.bmstu.akkaApp.Test.TestPackageMessage;

import akka.pattern.PatternsCS;
import akka.routing.RoundRobinPool;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;

import java.io.IOException;
import java.util.concurrent.CompletionStage;

/**
 * Hello world!
 *
 */
public class App {

    private ActorRef storeActor, testPackageActor, testPerformerRouter;

    private final String STORE_ACTOR = "storeActor";
    private final String TEST_PACKAGE_ACTOR = "testPackageActor";
    private final String TEST_PERFORMER_ROUTER = "testPerformerRouter";

    private static final String domain = 'localhost';
    private static final int port = 8080;

    private WebServer(final ActorSystem system) {
        storeActor = system.actorOf(Props.create(StoreActor.class), STORE_ACTOR);
        testPackageActor = system.actorOf(Props.create(TestPackageActor.class), TEST_PACKAGE_ACTOR);
        testPerformerRouter = system.actorOf(new RoundRobinPool(5).props(Props.create(TestActor.class)), TEST_PERFORMER_ROUTER);
    }

    private Route createRoute() {
        return route(
              get(() -> parameter("packageID", (packageID) -> {
                            CompletionStage<Object> result = PatternsCS.add(storeActor, new GetMessage(Integer.parseInt(packageID)), 5000);
                            return completeOKWithFuture(result, Jackson.marshaller());
                        })
              ),
              post(() -> entity(Jackson.unmarshaller(TestPackageMessage.class), msg -> {
                                  TestPackageMessage.tell(msg, ActorRef.noSender());
                                  return complete("Test started");
                         })
              )
        );
    }

    public static void main( String[] args ) {
        ActorSystem system = ActorSystem.create("routes");

        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);


        WebServer app = new WebServer(system);

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute().flow(system, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(
            routeFlow,
            ConnectHttp.toHost(domain,port),
            materializer
        )

        System.out.println("Server is started at http://localhost:8080");

        System.in.read();

        binding
              .thenCompose(ServerBinding::unbind)
              .thenAccept(unbound -> system.terminate());

        System.out.println("Server off")
    }
}
