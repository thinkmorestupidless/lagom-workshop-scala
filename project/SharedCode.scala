package sbtstudent

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import sbt.{IO => sbtio, _}

import scala.Console
import scala.util.matching.Regex

object SharedCode {
  object Settings {
    val deactivatedExerciseFolderName = ".deactivatedExercises"
  }

  object FoldersOnly {
    def apply() = new FoldersOnly
  }
  class FoldersOnly extends java.io.FileFilter {
    override def accept(f: File): Boolean = f.isDirectory
  }

  val ExercisePathSpec: Regex = """(.*/exercise_)(\d{3})(_\w+)$""".r
  val ExerciseNameSpec: Regex = """(exercise_)(\d{3})(_\w+)$""".r
  val InactivatedExerciseNameSpec: Regex = """.*exercise_[0-9][0-9][0-9]_\w+\.zip$""".r

  def isExerciseFolder(folder: File): Boolean = {
    ExercisePathSpec.findFirstIn(folder.getPath).isDefined
  }

  def getActiveExerciseNames(masterRepo: File): Vector[String] = {
    val exerciseFolders = sbtio.listFiles(masterRepo, FoldersOnly()).filter(isExerciseFolder)
    exerciseFolders.map(folder => folder.getName).toVector.sorted
  }

  object AllFiles {
    def apply() = new FoldersOnly
  }
  class AllFiles extends java.io.FileFilter {
    override def accept(f: File): Boolean = true
  }

  object NoTargetFolders {
    def apply() =  new NoTargetFolders
  }

  class NoTargetFolders extends java.io.FileFilter {
    override def accept(f: File): Boolean = !(f.isDirectory && f.getName == "target")
  }

  def fileList(base: File): Vector[File] = {
    @scala.annotation.tailrec
    def fileList(filesSoFar: Vector[File], folders: Vector[File]): Vector[File] = {
      val subs = (folders foldLeft Vector.empty[File]) {
        case (tally, folder) =>
          tally ++ sbtio.listFiles(folder, NoTargetFolders())
      }
      subs.partition(_.isDirectory) match {
        case (rem, result) if rem.isEmpty => filesSoFar ++ result
        case (rem, tally) => fileList(filesSoFar ++ tally, rem)
      }
    }

    val (seedFolders, seedFiles) = sbtio.listFiles(base).partition(_.isDirectory)
    fileList(seedFiles.toVector, seedFolders.toVector)
  }

  def withZipFile(state: State, exercise: String)(transformState: () => State): State = {
    val cueFolder = new sbt.File(new sbt.File(Project.extract(state).structure.root), s".cue")
    val srcZip = new sbt.File(cueFolder, s"${exercise}.zip")
    val unzippedSrc = new sbt.File(cueFolder, s"${exercise}")
    sbt.IO.unzip(srcZip, cueFolder)
    val newState = transformState()
    sbt.IO.delete(unzippedSrc)
    newState
  }

  def zipSolution(exFolder: File, removeOriginal: Boolean = false): Unit = {
    val fl = fileList(exFolder).map(f => (f, sbtio.relativize(exFolder.getParentFile, f).get))
    val zipFile = new File(exFolder.getParentFile, s"${exFolder.getName}.zip")
    sbtio.zip(fl, zipFile)
    if (removeOriginal) sbtio.delete(exFolder)
  }

  def getExercisesInMaster(projectBaseFolder: File, inactiveExercisesFolder: File): (Vector[String], Vector[String]) = {

    def isZippedExerciseFolder(folder: File): Boolean = {
      InactivatedExerciseNameSpec.findFirstIn(folder.getPath).isDefined
    }
    val hiddenExercises = sbtio.listFiles(inactiveExercisesFolder).filter(isZippedExerciseFolder).map(_.getName.replaceAll(".zip", "")).toVector.sorted
    val activeExercises = getActiveExerciseNames(projectBaseFolder)
    (activeExercises, hiddenExercises)
  }

  def deactivateExercises(projectBaseFolder: File, selectedExercises: Seq[String], hidingFolder: File): Unit = {
    selectedExercises.foreach { exercise =>
        val srcFolder = new File(projectBaseFolder, exercise)
        val exerciseFolder = new File(hidingFolder, exercise)
        sbtio.move(srcFolder, exerciseFolder)
        zipSolution(exerciseFolder, removeOriginal = true)
    }
  }

  def activateExercise(projectBaseFolder: File, exerciseName: Option[String], hidingFolder: File): Unit = {
    val exerciseZip = s"${exerciseName.get}.zip"
    sbtio.unzip(new File(hidingFolder, s"${exerciseName.get}.zip"), projectBaseFolder)
    sbtio.delete(new File(hidingFolder, exerciseZip))
  }

  def getInactiveExercisesFolder(state: State): File = {
    val deactivatedExerciseFolder = new sbt.File(new sbt.File(Project.extract(state).structure.root), Settings.deactivatedExerciseFolderName)
    if (! deactivatedExerciseFolder.exists) sbtio.createDirectory(deactivatedExerciseFolder)
    deactivatedExerciseFolder
  }

  def activateExerciseNr(state: State, nr: Int): State = {
    val projectBaseFolder = new sbt.File(Project.extract(state).structure.root)
    val inactiveExercisesFolder = getInactiveExercisesFolder(state)
    val (active, inactive) = getExercisesInMaster(projectBaseFolder, inactiveExercisesFolder)
    val activeExercisePrefix = f"exercise_$nr%03d.*"
    val activeExercisePrefixSpec = activeExercisePrefix.r
    val exInActive = active.find(ex => activeExercisePrefixSpec.findFirstIn(ex).isDefined)
    val exInInactive = inactive.find(ex => activeExercisePrefixSpec.findFirstIn(ex).isDefined)
    val exerciseName = exInActive orElse exInInactive
    if (exerciseName.isDefined) {
      deactivateExercises(projectBaseFolder, active, inactiveExercisesFolder)
      activateExercise(projectBaseFolder, exerciseName, inactiveExercisesFolder)
      genBuildFile(projectBaseFolder, Vector(exerciseName.get))
      Console.println(Console.GREEN + "[INFO] " + Console.RESET + s"Activated exercise ${Console.YELLOW}${exerciseName.get} ${Console.RESET}")
      Command.process(";reload", state)
    } else {
      Console.println(Console.RED + "[ERROR] " + Console.YELLOW + s"No exercise with number $nr" + Console.RESET)
      state
    }
  }

  def exerciseProjects(exercises: Vector[String], multiJVM: Boolean): String = {
    (multiJVM) match {
      case (false) =>
        exercises.map {exercise =>
          s"""lazy val $exercise = project
             |  .settings(CommonSettings.commonSettings: _*)
             |  .dependsOn(common % "test->test;compile->compile")
             |""".stripMargin
        }.mkString("", "\n", "\n")
      case (true) =>
        exercises.map {exercise =>
          s"""lazy val $exercise = project
             |  .settings(SbtMultiJvm.multiJvmSettings: _*)
             |  .settings(CommonSettings.commonSettings: _*)
             |  .configs(MultiJvm)
             |  .dependsOn(common % "test->test;compile->compile")
             |""".stripMargin
        }.mkString("", "\n", "\n")
    }
  }

  def genBuildFile(projectBaseFolder: File, exercises: Vector[String], multiJVM: Boolean = false): Unit = {
    val buildDef =
      s"""
         |lazy val base = (project in file("."))
         |  .aggregate(
         |    common,
         |${exercises.mkString("    ", ",\n    ", "")}
         | )
         |  .settings(CommonSettings.commonSettings: _*)
         |${if (multiJVM)
           s"""  .settings(SbtMultiJvm.multiJvmSettings: _*)
              |  .configs(MultiJvm)""".stripMargin else ""}
              |
              |lazy val common = project
              |  .settings(CommonSettings.commonSettings: _*)
              |
              |${exerciseProjects(exercises, multiJVM)}""".stripMargin

    dumpStringToFile(buildDef, new File(projectBaseFolder, "build.sbt") getPath)

  }

  def dumpStringToFile(string: String, filePath: String): Unit = {
    Files.write(Paths.get(filePath), string.getBytes(StandardCharsets.UTF_8))
  }

  def activateAllExercises(state: State): State = {
    val projectBaseFolder = new sbt.File(Project.extract(state).structure.root)
    val inactiveExercisesFolder = getInactiveExercisesFolder(state)
    val (active, inactive) = getExercisesInMaster(projectBaseFolder, inactiveExercisesFolder)
    for {
      exercise <- inactive
    } activateExercise(projectBaseFolder, Some(exercise), inactiveExercisesFolder)
    genBuildFile(projectBaseFolder, (active ++ inactive).sorted)
    val newState = Command.process(";reload", state)
    Console.println(Console.GREEN + "[INFO] " + Console.RESET + s"Activated all exercise${Console.RESET}")
    newState
  }
}