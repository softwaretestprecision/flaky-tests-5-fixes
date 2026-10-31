package com.softwaretestprecision.flaky;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Map;
import java.util.List;

/**
 * Video 3: the same five fixes, in Selenium Java.
 * Tests in group "flaky" are excluded by default (testng.xml).
 * Run them on purpose with: mvn test -Dsuite=testng-flaky.xml
 */
public class FiveFixesTest {

    private static final String INTERNET = "https://the-internet.herokuapp.com";
    private static final String SAUCE = "https://www.saucedemo.com";

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        ChromeOptions options = new ChromeOptions();
        if (!"false".equals(System.getProperty("headless"))) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1280,900", "--no-sandbox", "--disable-dev-shm-usage");
        // Turn off Chrome's password manager. Its "password found in a data breach" dialog can
        // appear after a login and silently block clicks. Selenium can't see browser dialogs.
        options.setExperimentalOption("prefs", Map.of(
                "credentials_enable_service", false,
                "profile.password_manager_enabled", false,
                "profile.password_manager_leak_detection", false));
        driver = new ChromeDriver(options);
        // Fix 0 (bonus): keep implicit wait at 0 and use explicit waits. Mixing both gives unpredictable timing.
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (driver != null) driver.quit();
    }

    private void loginToSauce() {
        driver.get(SAUCE);
        driver.findElement(By.cssSelector("[data-test='username']")).sendKeys("standard_user");
        driver.findElement(By.cssSelector("[data-test='password']")).sendKeys("secret_sauce");
        driver.findElement(By.cssSelector("[data-test='login-button']")).click();
        wait.until(ExpectedConditions.urlContains("inventory"));
    }

    // ---- Fix 1: no fixed sleeps --------------------------------------------------------
    @Test(groups = "flaky")
    public void fix1Flaky_threadSleep() throws InterruptedException {
        driver.get(INTERNET + "/dynamic_loading/2");
        driver.findElement(By.cssSelector("#start button")).click();
        Thread.sleep(2000); // a guess - sometimes too short, always wasted time when too long
        Assert.assertEquals(driver.findElement(By.id("finish")).getText(), "Hello World!");
    }

    @Test
    public void fix1Fixed_explicitWait() {
        driver.get(INTERNET + "/dynamic_loading/2");
        driver.findElement(By.cssSelector("#start button")).click();
        WebElement finish = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("finish")));
        Assert.assertEquals(finish.getText(), "Hello World!");
    }

    // ---- Fix 2: wait for the condition, not a one-time read ----------------------------
    @Test(groups = "flaky")
    public void fix2Flaky_oneTimeRead() {
        driver.get(INTERNET + "/dynamic_loading/1");
        driver.findElement(By.cssSelector("#start button")).click();
        Assert.assertTrue(driver.findElement(By.id("finish")).isDisplayed()); // checked once, too early
    }

    @Test
    public void fix2Fixed_waitForVisibility() {
        driver.get(INTERNET + "/dynamic_loading/1");
        driver.findElement(By.cssSelector("#start button")).click();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("finish"), "Hello World!"));
        Assert.assertTrue(driver.findElement(By.id("finish")).isDisplayed());
    }

    // ---- Fix 3: stable locators -------------------------------------------------------
    @Test(groups = "flaky")
    public void fix3Flaky_positionalLocator() {
        loginToSauce();
        new Select(driver.findElement(By.cssSelector("[data-test='product-sort-container']"))).selectByValue("za");
        driver.findElement(By.xpath("(//div[@class='inventory_item'])[1]//button")).click();
        driver.findElement(By.className("shopping_cart_link")).click();
        Assert.assertEquals(driver.findElement(By.className("inventory_item_name")).getText(), "Sauce Labs Backpack");
    }

    @Test
    public void fix3Fixed_dataTestLocator() {
        loginToSauce();
        new Select(driver.findElement(By.cssSelector("[data-test='product-sort-container']"))).selectByValue("za");
        driver.findElement(By.cssSelector("[data-test='add-to-cart-sauce-labs-backpack']")).click();
        driver.findElement(By.cssSelector("[data-test='shopping-cart-link']")).click();
        WebElement name = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[data-test='inventory-item-name']")));
        Assert.assertEquals(name.getText(), "Sauce Labs Backpack");
    }

    // ---- Fix 4: independent tests -----------------------------------------------------
    @Test(groups = "flaky")
    public void fix4Flaky_dependsOnEarlierTest() {
        // Assumes another test logged in and added an item. With a fresh driver per test it fails.
        driver.get(SAUCE + "/cart.html");
        List<WebElement> items = driver.findElements(By.cssSelector("[data-test='inventory-item']"));
        Assert.assertEquals(items.size(), 1);
    }

    @Test
    public void fix4Fixed_createsItsOwnState() {
        loginToSauce();
        driver.findElement(By.cssSelector("[data-test='add-to-cart-sauce-labs-bike-light']")).click();
        driver.findElement(By.cssSelector("[data-test='shopping-cart-link']")).click();
        wait.until(ExpectedConditions.urlContains("cart"));
        Assert.assertEquals(driver.findElements(By.cssSelector("[data-test='inventory-item']")).size(), 1);
    }

    // ---- Fix 5: wait for the real state change ------------------------------------------
    @Test(groups = "flaky")
    public void fix5Flaky_typesWhileDisabled() {
        driver.get(INTERNET + "/dynamic_controls");
        driver.findElement(By.cssSelector("#input-example button")).click();
        WebElement input = driver.findElement(By.cssSelector("#input-example input"));
        input.sendKeys("hello"); // throws ElementNotInteractableException while still disabled
        Assert.assertEquals(input.getDomProperty("value"), "hello");
    }

    @Test
    public void fix5Fixed_waitUntilEnabled() {
        driver.get(INTERNET + "/dynamic_controls");
        driver.findElement(By.cssSelector("#input-example button")).click();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("message"), "It's enabled!"));
        WebElement input = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#input-example input")));
        input.sendKeys("hello");
        Assert.assertEquals(input.getDomProperty("value"), "hello");
    }
}
