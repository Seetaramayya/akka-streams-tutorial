package tutorial.akka.streams.graph

import akka.actor.ActorSystem
import akka.stream.{ ClosedShape, UniformFanInShape }
import akka.stream.javadsl.MergePreferred
import akka.stream.scaladsl.{ Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip, ZipWith }

object FibnoacciExample extends App {
  implicit val system = ActorSystem()

  val fibnoacciGenerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._
    val source1Shape = builder.add(Flow[Int])
    val source2Shape = builder.add(Flow[Int])
    val broadcast = builder.add(Broadcast[(Int, Int)](3))
    val previousElementShape = builder.add(Flow[(Int, Int)].map(_._1))

    val currentElementShape = builder.add(Flow[(Int, Int)].map(_._2))
    val finalOutput = builder.add(Flow[(Int, Int)].map(_._2))
    val merge1Shape = builder.add(MergePreferred.create[Int](1))
    val merge2Shape = builder.add(MergePreferred.create[Int](1))
    val zipWithShape = builder.add(ZipWith[Int, Int, (Int, Int)] { (in1, in2) =>
      Thread.sleep(600)
      (in2, in1 + in2)
    })
    // format: OFF
    source1Shape ~> merge1Shape ~> zipWithShape.in0
    source2Shape ~> merge2Shape ~> zipWithShape.in1
                                   zipWithShape.out ~> broadcast.in
                                                       broadcast.out(0) ~> previousElementShape ~> merge1Shape.preferred
                                                       broadcast.out(1) ~> currentElementShape ~> merge2Shape.preferred
                                                       broadcast.out(2) ~> finalOutput
    
    // format: ON
    UniformFanInShape(finalOutput.out, source1Shape.in, source2Shape.in)
  }

  RunnableGraph
    .fromGraph {
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._
        val source1Shape = builder.add(Source.single[Int](21))
        val source2Shape = builder.add(Source.single[Int](34))
        val sinkShape = builder.add(Sink.foreach(println))
        val fibnoacciShape = builder.add(fibnoacciGenerator)
        source1Shape ~> fibnoacciShape
        source2Shape ~> fibnoacciShape ~> sinkShape
        ClosedShape
      }
    }
    .run()

}
