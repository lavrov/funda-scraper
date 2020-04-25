import mill.define.Target
import mill.define.TaskModule
import $ivy.`org.seleniumhq.selenium:selenium-firefox-driver:3.9.1`
import $ivy.`org.seleniumhq.selenium:selenium-support:3.9.1`
import org.openqa.selenium.firefox.FirefoxOptions

object scraper extends TaskModule {

  def defaultCommandName() = "run"

  def run(searchPath: String, headless: Boolean = true) = Target.command {

    import org.openqa.selenium.By
    import org.openqa.selenium.support.ui.{WebDriverWait, ExpectedConditions}
    import org.openqa.selenium.firefox.FirefoxDriver

    import java.time.Instant
    import java.time.temporal.ChronoUnit

    import scala.jdk.CollectionConverters._

    val currentTime = Instant.now()
    val searchUrl = s"https://funda.nl$searchPath"
    val outputDirectory = {
      val dataDirectory = os.pwd / "data"
      val searchResultDirectory = os.Path(searchPath).segments.foldLeft(dataDirectory)(_ / _)
      searchResultDirectory / currentTime.truncatedTo(ChronoUnit.SECONDS).toString
    }

    os.makeDir.all(outputDirectory)

    val driver = new FirefoxDriver(
      new FirefoxOptions().setHeadless(headless)
    )
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
