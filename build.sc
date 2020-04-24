import mill._
import mill.scalalib._

object scraper extends ScalaModule {

  def scalaVersion = "2.13.2"

  def ivyDeps = Agg(
    ivy"com.lihaoyi::os-lib:0.7.0",
    ivy"org.seleniumhq.selenium:selenium-firefox-driver:3.9.1",
    ivy"org.seleniumhq.selenium:selenium-support:3.9.1",
  )

}
