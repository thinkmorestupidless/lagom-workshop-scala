# Lagom Workshop (Scala)

## Summary of using the course tools

* From the project root type sbt to begin the course
* Type man e to see the instructions for the exercise
* Type run when you think you've got the solution
* Type nextExercise pullSolution reload to progress to the next exercise when you are satisfied

## Long winded version

This project has been made with "Studentify" course generation tools.

In order to progress through the exercises, you'll need to know a few commands.

First thing, we will be using sbt to run the course tool.

To start the course, simply type sbt

At this point, you should see the first exercise load up and it will look something like man [e] > Wallet > exercise_000_initial_state

This tells you which exercise you are currently running.

To see the instructions for the exercise, type man e

This will show you the instructions for the exercise that you need to complete.

To run the exercise after completing any code changes, type run

Running the exercise should create some output on the console and you may also need to use the browser, terminal, or websocket tool to validate that the exercise has been completed properly.

To progress to the next exercise type nextExercise pullSolution reload

This will progress you to the next exercise and reload sbt so that it's ready for your changes. At that point you repeat the steps from man e

There are other commands available to use but we will not be using in this course

If you forget the commands at any time, simply type man