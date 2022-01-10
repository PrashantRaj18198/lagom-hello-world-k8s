import com.lightbend.lagom.core.LagomVersion
import kubeyml.deployment._
import kubeyml.deployment.api._
import kubeyml.deployment.plugin.Keys._
import scala.concurrent.duration._
import com.amazonaws.regions.{Region, Regions}

enablePlugins(KubeDeploymentPlugin)
enablePlugins(KubeServicePlugin)
enablePlugins(DockerPlugin)
enablePlugins(JavaAppPackaging)

organization in ThisBuild := "com.example"
version in ThisBuild := sys.props.getOrElse("version", default = "1.0.1-SNAPSHOT")

region           in ecr := Region.getRegion(Regions.US_EAST_1)
repositoryName   in ecr := (packageName in Docker).value
localDockerImage in ecr := (packageName in Docker).value + ":" + (version in Docker).value

push in ecr <<= (push in ecr) dependsOn (publishLocal in Docker)

// version in ThisBuild ~= (_.replace('+', '-'))

// dynver in ThisBuild ~= (_.replace('+', '-'))

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.12.4"

val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.4" % Test
val akkaDiscovery = "com.lightbend.lagom" %% "lagom-scaladsl-akka-discovery-service-locator" % LagomVersion.current
val akkaKubernetes = "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % "1.0.0"
val postgres = "org.postgresql" % "postgresql" % "42.2.8"
val h2 = "com.h2database" % "h2" % "1.4.199"

dockerBaseImage := "adoptopenjdk/openjdk8"

lagomCassandraEnabled in ThisBuild := false

lazy val `lagom-hello-world-k8s` = (project in file("."))
  .aggregate(
    `lagom-hello-world-k8s-api`,
    `lagom-hello-world-k8s-impl`,
    `lagom-hello-world-k8s-stream-api`,
    `lagom-hello-world-k8s-stream-impl`,
  )
  .settings(
    dockerRepository := Some("prashantraj18198"),
    publishArtifact := false,
  )

lazy val `lagom-hello-world-k8s-api` =
  (project in file("lagom-hello-world-k8s-api"))
    .settings(
      publishArtifact := false,
      libraryDependencies ++= Seq(
        lagomScaladslApi
      )
    )

lazy val `lagom-hello-world-k8s-impl` =
  (project in file("lagom-hello-world-k8s-impl"))
    .enablePlugins(LagomScala)
    .settings(
      dockerRepository := Some("prashantraj18198"),
      dockerExposedPorts ++= Seq(9000, 9001),
      libraryDependencies ++= Seq(
        lagomScaladslPersistenceJdbc,
        lagomScaladslKafkaBroker,
        lagomScaladslTestKit,
        macwire,
        scalaTest,
        akkaDiscovery,
        akkaKubernetes,
        postgres,
        h2
      )
    )
    .settings(lagomForkedTestSettings)
    .dependsOn(`lagom-hello-world-k8s-api`)

lazy val `lagom-hello-world-k8s-stream-api` =
  (project in file("lagom-hello-world-k8s-stream-api"))
    .settings(
      publishArtifact := false,
      libraryDependencies ++= Seq(
        lagomScaladslApi
      )
    )

lazy val `lagom-hello-world-k8s-stream-impl` =
  (project in file("lagom-hello-world-k8s-stream-impl"))
    .enablePlugins(LagomScala)
    .settings(
      dockerRepository := Some("prashantraj18198"),
      libraryDependencies ++= Seq(
        lagomScaladslTestKit,
        macwire,
        scalaTest
      )
    )
    .dependsOn(`lagom-hello-world-k8s-stream-api`, `lagom-hello-world-k8s-api`)

val deploymentName = sys.props.getOrElse("deploymentName", default = "hello-world")
val deploymentNamespace = sys.props.getOrElse("namespace", default = "default")
val secretsName = sys.props.getOrElse("secretName", default = "myservice-test-secrets")

// val `deployment-settings` = 
//   (project in file("."))
//     .enablePlugins(KubeDeploymentPlugin)
//     .enablePlugins(KubeServicePlugin)
//     .settings(
//       Seq(
//         kube / namespace := deploymentNamespace, //default is ThisProject / name 
//         kube / application := deploymentName, //default is ThisProject / name
//         kube / livenessProbe := HttpProbe(HttpGet("/ready", port = 80, httpHeaders = List.empty)),
//         kube / resourceLimits := Resource(Cpu.fromCores(2), Memory(2048+512)),
//         kube / resourceRequests := Resource(Cpu(500), Memory(512)),
//         //if you want you can use something like the below to modify any part of the deployment by hand
//         kube / deployment := (kube / deployment).value.pullDockerImage(IfNotPresent)
//       )
//     )
//     .settings(
//       publishArtifact := false,
//       publish := false
//     )

// deploy.namespace(deploymentNamespace)
//     .service(deploymentName)
//      .withImage("lagom-hello-world-k8s-impl")
//     .withProbes(
//       livenessProbe = HttpProbe(HttpGet("/", port = 80, httpHeaders = List.empty)), 
//       readinessProbe = HttpProbe(HttpGet("/", port = 80, httpHeaders = List.empty), failureThreshold = 10)
//     ).replicas(3)
//     .pullDockerImage(IfNotPresent)
//     .addPorts(List(Port(None, 80)))
//     .deploymentStrategy(RollingUpdate(0, 1))

val deploymentSettings = Seq(
        kube / namespace := deploymentNamespace, //default is ThisProject / name 
        kube / application := deploymentName, //default is ThisProject / name
        kube / livenessProbe := HttpProbe(HttpGet("/ready", port = 80, httpHeaders = List.empty)),
        kube / resourceLimits := Resource(Cpu.fromCores(2), Memory(2048+512)),
        kube / resourceRequests := Resource(Cpu(500), Memory(512)),
        //if you want you can use something like the below to modify any part of the deployment by hand
        kube / deployment := (kube / deployment).value.pullDockerImage(IfNotPresent)
)