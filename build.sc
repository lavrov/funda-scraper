import $ivy.`org.seleniumhq.selenium:selenium-firefox-driver:3.9.1`
import $ivy.`org.seleniumhq.selenium:selenium-support:3.9.1`
import $ivy.`org.http4s::http4s-core:0.21.3`

import mill.define.{Target, TaskModule}

import org.http4s.Uri

import java.time.Instant
import java.time.temporal.ChronoUnit

import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions}
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.{WebDriverWait, ExpectedConditions}

import scala.jdk.CollectionConverters._

object scraper extends TaskModule {

  def defaultCommandName() = "run"

  def run(url: String, headless: Boolean = true) = Target.command {

    val uri = Uri.unsafeFromString(url)

    val currentTime = Instant.now().truncatedTo(ChronoUnit.SECONDS)
    val dataDirectory = os.pwd / "data"
    val outputDirectory = {
      val searchResultDirectory = os.Path(uri.path).segments.foldLeft(dataDirectory)(_ / _)
      searchResultDirectory / currentTime.toString
    }

    os.makeDir.all(outputDirectory)

    val driver =
      new FirefoxDriver(
        new FirefoxOptions().setHeadless(headless)
      )

    val waitDriver =
      new WebDriverWait(driver, 10)

    object selectors {
      val `search-results` = By.className("search-results")
      val `search-result`  = By.className("search-result")
      val `object-primary` = By.className("object-primary")
    }

    def scrapeSearchResults(url: String, index: Int = 0): Unit = {

      println(s"INFO Navigating to $url")

      driver.navigate.to(url)

      waitDriver.until(ExpectedConditions.presenceOfElementLocated(selectors.`search-results`))

      val pageSource = driver.getPageSource

      os.write(outputDirectory / s"$index.html", pageSource)

      val nextPageUrl = driver
        .findElementByClassName("pagination")
        .findElements(By.cssSelector("a[rel='next']")).asScala.headOption
        .map(_.getAttribute("href"))

      val homeUrls =
        for {
          searchResults <- driver.findElements(selectors.`search-results`).asScala
          searchResult  <- searchResults.findElements(selectors.`search-result`).asScala
          anchor        <- searchResult.findElements(By.cssSelector("a[data-search-result-item-anchor]")).asScala.take(1)
          url           <- Option(anchor.getAttribute("href")).toIterable
        } yield url

      homeUrls.foreach { url =>
        scrapeHousePage(url)
      }

      for (url <- nextPageUrl) scrapeSearchResults(url, index + 1)
    }

    def scrapeHousePage(url: String): Unit = {

      val uri = Uri.unsafeFromString(url)

      println(s"INFO Navigating to $url")

      driver.navigate().to(url)

      waitDriver.until(ExpectedConditions.presenceOfElementLocated(selectors.`object-primary`))

      val pageSource = driver.getPageSource

      val outputFile = os.Path(uri.path).segments.foldLeft(dataDirectory)(_ / _) / s"$currentTime.html"

      os.write(outputFile, pageSource, createFolders = true)
    }

    try
      scrapeSearchResults(url)
    finally
      driver.close()
  }

}
