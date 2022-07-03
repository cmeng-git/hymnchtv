/*
 * hymnchtv: COG hymns' lyrics viewer and player client
 * Copyright 2020 Eng Chong Meng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cog.hymnchtv.mediaconfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Class supports SingleTap, DoubleTap and LongPress for the given touched view
 * $ sudo docker run -it -p 8050:8050 --rm scrapinghub/splash
 * $ sudo docker run -it -p 8050:8050 --rm scrapinghub/splash
 *
 * @author Eng Chong Meng
 */
public class WebScraper
{
    public static String getURLSource(String url) throws IOException
    {
        URL urlObject = new URL(url);
        URLConnection urlConnection = urlObject.openConnection();
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");

        return toString(urlConnection.getInputStream());
    }

    public static String toString(InputStream inputStream) throws IOException
    {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String inputLine;
            StringBuilder stringBuilder = new StringBuilder();
            while ((inputLine = bufferedReader.readLine()) != null) {
                inputLine = inputLine.trim().replaceAll("  ", " ");
                stringBuilder.append(inputLine);
            }
            return stringBuilder.toString();
        }
    }

    /**
     * Fetch site source based on OKHTTP
     * TODO: QQ site contain error i.e. // ====== OkHttpClient === ", }" json error
     *
     * @param url The site url
     * @return the site source
     * @throws IOException IO exception
     */
    public static String getURLSourceOkHttp(String url) throws IOException
    {
        final OkHttpClient client = new OkHttpClient.Builder().build();
        final Request request = new Request.Builder().url(url).build();
        ResponseBody responseBody = client.newCall(request).execute().body();
        try (final Response response = client.newCall(request).execute()) {
            String htmlContent = Objects.requireNonNull(response.body()).string()
                    .trim()
                    .replaceAll("  ", " ")
                    .replaceAll("\\n", "")
                    .replaceAll("\\\\\"", "\"");
            return htmlContent;
        }
    }

    /**
     * ============= scrapingAnt ===== 10,000 records only for free account
     * Fetch site source via scrapingAnt via x-api-key; working OK for notion
     *
     * @param url The site url
     * @return the site source
     * @throws IOException IO exception
     */
    public static String getURLSourceAnt(String url) throws IOException
    {
        String content;
        final OkHttpClient client = new OkHttpClient.Builder().build();

        final String baseUrl = "https://api.scrapingant.com/v1/general?url=";
        final String encodedTarget = URLEncoder.encode(url, "UTF-8");
        final Request request = new Request.Builder()
                .addHeader("x-api-key", "2b15dcd942d04757949ecb2c8fcde50f")
                .url(baseUrl + encodedTarget)
                .build();

        try (final Response response = client.newCall(request).execute()) {
            content = Objects.requireNonNull(response.body()).string();

            content = content.trim()
                    .replaceAll(" {2}", " ")
                    .replaceAll("\\\\n", "")
                    .replaceAll("\\\\\"", "\"");
            // final Gson gson = new Gson();
            // return gson.fromJson(response.body().string(), ScrapingAntResponse.class);
        }
        return content;
    }

//    /**
//     * ============ playwright method ===========
//     * Fetch site source using playwright; required patch for android in
//     * DriverJar and PlaywrightImpl;
//     * TODO: still having connection disconnected
//     *
//     * @param url The site url
//     * @throws IOException IO exception
//     * @return the site source
//     */
//    public static String getSourcePlaywright(String url) throws IOException
//    {
//        String content = null;
//        // Android doesn't need Playwright to install web browsers => "1'
//        Map<String, String> env = new HashMap<>();
//        env.put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "0");
//        Playwright.CreateOptions options = new Playwright.CreateOptions().setEnv(env);
//
//        try (Playwright playwright = Playwright.create(options)) {
//            final BrowserType chromium = playwright.chromium();
//            final Browser browser = chromium.launch();
//            final Page page = browser.newPage();
//            page.navigate(url);
//            content = page.content();
//            browser.close();
//
//        } catch (Exception e) {
//            Timber.w(e, "Playwright exception: %s", e.getMessage());
//        }
//        return content;
//    }

//    /**
//     * Fetch site source based on java ported Puppeteer
//     * TODO: required executing chrome...; instead of
//     * adb shell am start --user 0 -n com.android.chrome/com.google.android.apps.chrome.Main
//     * adb shell am start -n com.android.chrome/org.chromium.chrome.browser.ChromeTabbedActivity -d "https://www.google.com"
//     *
//     * @param url The site url
//     * @return the site source
//     */
//    public static String getSourceJvppeteer(String url)
//    {
//        String content = null;
//        String path = new String("F:\\java\\vuejs\\puppeteer\\.local-chromium\\win64-722234\\chrome-win\\chrome.exe".getBytes(), StandardCharsets.UTF_8);
//
//        try {
//            //自动下载，第一次下载后不会再下载
//            // BrowserFetcher.downloadIfNotExist(null);
//            ArrayList<String> argList = new ArrayList<>();
//            argList.add("--no-sandbox");
//            argList.add("--disable-setuid-sandbox");
//
//            LaunchOptions options = new LaunchOptionsBuilder()
//                    .withArgs(argList)
//                    .withHeadless(true)
//                    // .withExecutablePath(path)
//                    .build();
//            options.setProduct("chrome");
//
//            Browser browser = Puppeteer.launch(options);
//            Page page = browser.newPage();
//            page.goTo(url);
//            content = page.content();
//            browser.close();
//
//        } catch (InterruptedException | IOException e) {
//            Timber.w(e, " Puppeteer exception: %s", e.getMessage());
//        }
//        return content;
//    }
//
//    /**
//     * Fetch site source based on htmlUnit for android; more work
//     *
//     * @param url The site url
//     * @return the site source
//     */
//    public static void getUrlSourceHtmlUnit(String url) throws IOException
//    {
//        WebClient webClient = new WebClient(BrowserVersion.CHROME);
//        webClient.getOptions().setJavaScriptEnabled(true);
//        webClient.getOptions().setThrowExceptionOnScriptError(false);
//        webClient.setAjaxController(new NicelyResynchronizingAjaxController());
//
//        // overcome problems in JavaScript
//        webClient.getOptions().setPrintContentOnFailingStatusCode(false);
//        webClient.setCssErrorHandler(new SilentCssErrorHandler());
//
//        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
//
//        try {
//            final HtmlPage page = webClient.getPage(url);
//            webClient.waitForBackgroundJavaScript(2000);
//            final DomElement element = page.getElementById("test");
//            Timber.d("Page Content: %S", page.asNormalizedText());
//        } catch (FailingHttpStatusCodeException | IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    // ============== htmlUnit alternate ========
//    @RequiresApi(api = Build.VERSION_CODES.N)
//    public static void getSourceHtmlUnit2(String url) throws IOException
//    {
//        WebClient webClient = new WebClient(BrowserVersion.CHROME);
//
//        webClient.getCookieManager().setCookiesEnabled(true);
//        webClient.getOptions().setJavaScriptEnabled(true);
//        webClient.getOptions().setTimeout(2000);
//        webClient.getOptions().setUseInsecureSSL(true);
//        // overcome problems in JavaScript
//        webClient.getOptions().setThrowExceptionOnScriptError(false);
//        webClient.getOptions().setPrintContentOnFailingStatusCode(false);
//        webClient.setCssErrorHandler(new SilentCssErrorHandler());
//        try {
//            final HtmlPage page = webClient.getPage("https://www.innoq.com/en/search/");
//            final HtmlTextInput searchField = htmlElementById(page,
//                    "q",
//                    HtmlTextInput.class).get();
//            // alternative to XPath: querySelector(".search-form__btn")
//            final HtmlButton searchButton = htmlElementByXPath(page,
//                    "//button[@class='search-form__btn' and @type='submit']",
//                    HtmlButton.class).get();
//            searchField.setValueAttribute("Scraping");
//
//            final HtmlPage resultPage = searchButton.click();
//            // in real life you may use LOGGER.debug()
//            //  System.out.println("HTML source: " + resultPage.getWebResponse().getContentAsString());
//            htmlElementsByCssClass(resultPage, ".search-result", HtmlAnchor.class).stream().forEach(System.out::println);
//        } catch (FailingHttpStatusCodeException | IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @RequiresApi(api = Build.VERSION_CODES.N)
//    public static <T> Optional<T> htmlElementByXPath(
//            DomNode node,
//            String xpath,
//            Class<T> type)
//    {
//        return node.getByXPath(xpath).stream()
//                .filter(o -> type.isAssignableFrom(o.getClass()))
//                .map(type::cast).findFirst();
//    }
//
//    @RequiresApi(api = Build.VERSION_CODES.N)
//    public static <T> List<T> htmlElementsByCssClass(
//            DomNode node,
//            String cssClass,
//            Class<T> type)
//    {
//        return node.querySelectorAll(cssClass).stream()
//                .filter(o -> type.isAssignableFrom(o.getClass()))
//                .map(type::cast).collect(Collectors.toList());
//    }
//
//    @RequiresApi(api = Build.VERSION_CODES.N)
//    public static <T> Optional<T> htmlElementById(
//            HtmlPage node,
//            String htmlTagId,
//            Class<T> type)
//    {
//        return Optional.ofNullable(node.getElementById(htmlTagId)).map(type::cast);
//    }
//
//
//    /**
//     * Get from source file - testing only
//     *
//     * @param url
//     * @return
//     * @throws IOException
//     */
//    public static String getSourceFromFile(String url) throws IOException
//    {
//        String mResult = null;
//        try {
//            InputStream in2 = HymnsApp.getGlobalContext().getResources().getAssets().open("nPageContent.html");
//            byte[] buffer2 = new byte[in2.available()];
//            if (in2.read(buffer2) != -1)
//                mResult = EncodingUtils.getString(buffer2, "utf-8");
//
//        } catch (IOException e) {
//            Timber.w("Page Content not available: %s", e.getMessage());
//        }
//        return mResult;
//    }
}



