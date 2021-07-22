package tutorail.akka.streams.operators;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;

import java.util.*;

public class StatefulMapConcat {
    public static void main(String[] args) {
    ActorSystem system = ActorSystem.apply();
    List<String> input = Arrays.asList("deny:banana", "deny:pear", "orange", "deny:banana", "banana", "pear", "banana");
    Source<String, NotUsed> fruitsAndDenyCommands = Source.from(input);

    Flow<String, String, NotUsed> denyFilterFlow =
        Flow.of(String.class)
            .statefulMapConcat(
                () -> {
                    Set<String> denyList = new HashSet<>();

                    return (element) -> {
                    if (element.startsWith("deny:")) {
                        denyList.add(element.substring("deny:".length()));
                        return Collections.emptyList(); // no element downstream when adding a deny listed keyword
                    } else if (denyList.contains(element)) {
                        return Collections
                            .emptyList(); // no element downstream if element is deny listed
                    } else {
                        return Collections.singletonList(element);
                    }
                    };
                });

    fruitsAndDenyCommands.via(denyFilterFlow).runForeach(System.out::println, system);
    }
}
