package org.ehoffman.testng.modules.webdriver.test;

import static org.fest.assertions.Assertions.*;

import java.io.File;
import java.net.BindException;
import java.util.concurrent.CountDownLatch;

import org.ehoffman.testing.module.webdriver.WebDriverGridModule;
import org.ehoffman.testing.module.webdriver.StaticWebdriverGridHelper;
import org.openqa.grid.common.exception.CapabilityNotPresentOnTheGridException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class WebdriverGridTest {

  @BeforeClass
  public void startHub() {
    System.setProperty("hubport", "4444");
    System.setProperty("seleniumhub", "http://localhost:4444/wd/hub");
    try {
      StaticWebdriverGridHelper.lauchGrid();
      StaticWebdriverGridHelper.lauchNode("http://localhost:4444/grid");
    } catch (Exception e) {
      // if this fails, it better be because the hub is already running.
      assertThat(e).isExactlyInstanceOf(BindException.class).hasMessage("Address already in use");
    }
  }

  @AfterClass
  public void stopHub() throws Exception {
    StaticWebdriverGridHelper.stopRemote();
    StaticWebdriverGridHelper.stopHub();
  }
  
  @Test
  public void missingIE6GridTest() throws Exception {
    WebDriver driver = null;
    try {
      WebDriverGridModule.IE6 module = new WebDriverGridModule.IE6();
      driver = (WebDriver) module.makeObject();
      assertThat(driver).isNotNull();
      assertThat(true).as("should not be reachable").isFalse();
    } catch (Throwable t) {
      assertThat(t).isExactlyInstanceOf(org.openqa.selenium.WebDriverException.class);
    } finally {
      if (driver != null) driver.close();
    }
  }

  @Test
  public void basicGridTest() throws Throwable {
    //TODO: fix this (auto installer?)
    WebDriver driver = null;
    try {
      WebDriverGridModule.Firefox module = new WebDriverGridModule.Firefox();
      driver = (WebDriver) module.makeObject();
      assertThat(driver).isNotNull();
    } catch (Throwable t){
      throw t;
    } finally {
      if (driver != null) driver.close();
    }
  }

  @Test
  public void testCanTakeScreenShotThroughGrid() throws Throwable {
    WebDriverGridModule.Firefox module = new WebDriverGridModule.Firefox();
    WebDriver driver = null;
    try {
      driver = (WebDriver) module.makeObject();
      driver.get("http://www.google.com");
      File screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
    } catch (Throwable t){
      throw t;
    } finally {
      if (driver != null) driver.close();
    }
  }

}
