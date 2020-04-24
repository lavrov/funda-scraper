import org.openqa.selenium.By
import org.openqa.selenium.support.ui.{WebDriverWait, ExpectedConditions}
import org.openqa.selenium.firefox.FirefoxDriver

import java.time.Instant

import scala.jdk.CollectionConverters._

object scrape {

  def main(args: Array[String]): Unit = {

    val currentTime = Instant.now()
    val searchPath = args(0)
    val searchUrl = s"https://funda.nl$searchPath"
    val outputDirectory = {
      val root = os.Path.expandUser(args.lift(1).getOrElse("~/funda"))
      os.Path(searchPath).segments.foldLeft(root)(_ / _) / currentTime.toString
    }

    os.makeDir.all(outputDirectory)

    val driver = new FirefoxDriver()
    val waitDriver = new WebDriverWait(driver, 10)

    object selectors {
      val `search-results` = By.className("search-results")
    }

    def scrapeSearchResults(url: String, index: Int = 0): Unit = {

      driver.navigate.to(url)

      waitDriver.until(ExpectedConditions.presenceOfElementLocated(selectors.`search-results`))

      val pageSource = driver.getPageSource()

      os.write(outputDirectory / s"$index.html", pageSource)

      val nextPageUrl = driver
        .findElementByClassName("pagination")
        .findElements(By.cssSelector("a[rel='next']")).asScala.headOption
        .map(_.getAttribute("href"))

      for (url <- nextPageUrl) scrapeSearchResults(url, index + 1)
    }

    try
      scrapeSearchResults(searchUrl)
    finally
      driver.close
  }
}
