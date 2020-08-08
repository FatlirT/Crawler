import org.apache.poi.ss.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;


/**
 *
 * The Crawler - designed to find prices of various household appliances across various providers.
 * In this version, the Crawler requires an Excel File which lists the target products
 * and the websites of the providers that the user wants to find the prices of.
 *
 * This Excel File must be formatted in a way such that the first row of the file is reserved
 * for the column headers, which are arranged as follows:
 *
 * Product Name, Product Code, Product Brand, Website 1, Website 2, ...... Website N.
 *
 * In later versions, it may support multiple other input streams such as
 * databases or CSV Files.
 *
 *
 * @author Fatlir Topalli
 * @version 1.0
 *
 */
public class Crawler {

    /**
     * The complete interface of the Crawler.
     * @param filePath  The file to open. Must not be null, but it cannot be null due to restrictions
     *                  set up in the GUI.
     * @param overwrite Whether to overwrite current prices with new ones.
     * @throws IOException This is either thrown as a result of the inability to load the Excel file
     *                     or the inability to connect to the internet.
     */
    public void updatePrices(String filePath, boolean overwrite) throws IOException {

        // The workbook.
        Workbook workbook = load(filePath);

        // Getting the first sheet.
        Sheet sheet = workbook.getSheetAt(0);

        // get column headers.
        Row columns = sheet.getRow(0);

        // iterate through products.
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {

            Row row = sheet.getRow(i);
            String code;
            String brand;
            String name;
            try{
                code = row.getCell(1).getStringCellValue().strip();
                brand = row.getCell(2).getStringCellValue().strip();
                name = row.getCell(0).getStringCellValue().strip();
            }catch (NullPointerException e){
                continue;
            }


            for (int j = 3; j < columns.getLastCellNum(); j++) {
                String website = columns.getCell(j).getStringCellValue().strip();
                Cell priceCell = row.getCell(j);
                if (priceCell == null || overwrite) {
                    Double price = findPrice(website, code, brand, name);
                    if (price != null) {
                        row.createCell(j);
                        row.getCell(j).setCellValue(price);
                    }
                    // if price null, reset non empty price cell to empty.
                    else{
                        try{ row.getCell(j).setBlank(); }
                        catch(NullPointerException e){
                            // no cell is present, do nothing.
                        }

                    }
                }
            }
        }

        writeToFile(filePath, workbook);
    }

    /**
     * This is a bridge method between the Class' interface and Main Logic.
     * It returns the price to be written to the file.
     * @param website Name of website featuring product.
     * @param code Product code.
     * @param brand Product brand.
     * @param name Product name.
     * @return The price of the product.
     * @throws IOException This is either thrown as a result of the inability to load the Excel file
     *                     or the inability to connect to the internet.
     */
    public Double findPrice(String website,String code, String brand, String name) throws IOException {

        HashSet<String> validUrls = getProductPages(website, code, brand, name);
        return priceScraper(validUrls, website);

    }

    /**
     * Searches google for the product using the product information and then gets all the urls
     * in which the product is featured.
     * Despite all the validation in place, sometimes an unscrapable page may be featured in this list.
     *
     * In future versions, we plan to move the scraping methods into separate classes for each website.
     * In doing so, more price scraping methods can be added for each website to allow us to scrape
     * prices from multiple different types of pages that the sites may have.
     *
     * @param website Name of website featuring product.
     * @param code Product code.
     * @param brand Product brand.
     * @param name Product name.
     * @return A set of valid URLs (URLs which address pages that the priceScraper can scrape).
     * @throws IOException This is thrown as a result of the inability to connect to a url.
     */
    private HashSet<String> getProductPages(String website, String code, String brand, String name) throws IOException {

        code = code.strip();

        String urlCode = code.replace(" ", "+");
        String googleUrl = ("http://www.google.com/search?q=" + website + "+" + urlCode + "+" + brand);

        Document googlePage = connect(googleUrl);

        Elements results = googlePage.getElementsByClass("ZINbbc xpd O9g5cc uUPGi");
        System.out.println(googlePage);
        System.out.println(results);

        HashSet<String> validUrls = new HashSet<>();
        String url;
        String title;
        String desc;
        for(Element element : results) {
            try {
                title = element.getElementsByTag("h3").first().text();
                url = URLDecoder.decode(element.getElementsByTag("a").first().attr("href").split("/url\\?q=")[1].split("&", 2)[0], StandardCharsets.UTF_8);

                desc = element.getElementsByClass("kCrYT").get(1).text();
            } catch (NullPointerException e) {
                continue;
            }
            if ((title.contains(code) || (title.contains(name) && desc.contains(code))) && url.contains(website) && !url.endsWith(".pdf")) {
                validUrls.add(url);
            }
        }
        return validUrls;
    }

    /**
     * This method contains the price scraping methods for each site.
     * When a new site is added, this is the only place where we must
     * update the Crawler program and add a scraping method for that class.
     * @param validUrls A list of valid urls that the price scraper can scrape.
     * @param website Name of website, to choose correct price scraping method.
     * @return The price found.
     * @throws IOException This is thrown as a result of the inability to connect to a url.
     */
    private Double priceScraper(HashSet<String> validUrls, String website) throws IOException {

        if(validUrls.isEmpty())
            return null;

        Collection<Double> prices = new ArrayList<>();

        for(String url : validUrls){

            Document productPage = connect(url);
            String priceString = null;
            try {
                priceString = switch (website) {
                    case ("argos.co.uk") -> productPage.getElementsByAttributeValue("itemprop", "price").first().attr("content");
                    case ("currys.co.uk") -> productPage.getElementsByClass("prd-amounts").first().getElementsByClass("current").first().text();
                    case ("ao.com") -> productPage.getElementById("productInformation").getElementsByAttributeValue("itemprop", "price").first().text();
                    case ("rdo.co.uk") -> productPage.getElementById("final-price").text();
                    case ("markselectrical.co.uk") -> productPage.getElementsByClass("price price-cashback").first().text();
                    default -> null;
                };
            } catch(NullPointerException e){
                System.out.println("Invalid url/could not find correct html tags to get price");
            }

            if(priceString != null) {
                prices.add(priceStringParser(priceString));
            }
        }
        if(prices.isEmpty())
            return null;
        return Collections.min(prices);

    }

    /**
     * This method is used to write a workbook to an Excel File.
     * @param filePath  The file to write to - in the context  of this program,
     *                  the file imported to read and the file which the data is written to
     *                  never differs, as we expect to be the case, so the filePath
     *                  passed to this method is the file path passed into the updatePrices
     *                  method that calls this one.
     * @param workbook The workbook to write to the specified file.
     * @throws IOException This is thrown as a result of the inability to access the file.
     */
    private void writeToFile(String filePath, Workbook workbook) throws IOException {

        File newFile = new File(filePath.split("\\.xlsx")[0] + "-Crawler.xlsx");
        FileOutputStream out = new FileOutputStream(newFile);

        workbook.write(out);

        out.flush();
        out.close();

        if (newFile.delete()) {
            System.out.println("Successful write to file!");
        } else {
            System.out.println("Write to file failed!");
        }

        // Closing the workbook.
        workbook.close();
    }

    /**
     * Load an Excel file into a workbook.
     * @param filePath  The file to open.
     * @return A workbook representation of the Excel File which can be edited in Java.
     * @throws IOException This is thrown as a result of the inability to access the file.
     */
    private Workbook load(String filePath) throws IOException {

        // Creating a Workbook from an Excel file (.xls or .xlsx).
        return WorkbookFactory.create(new File(filePath));

    }

    /**
     * Connect to a given url and get the page.
     * @param url The url we wish to connect to and get the page of.
     * @return A Document containing the web page of the given url.
     * @throws IOException This is thrown as a result of the inability to connect to the url.
     */
    private Document connect(String url) throws IOException {

        return Jsoup.connect(url).userAgent("mozilla/17.0").get();
        // later this method will contain: rotations for the User Agent, proxies and delays.
    }

    /**
     * Parse price strings into doubles.
     * @param price The price string to be parsed.
     * @return The parsed price in the required 'double' format.
     */
    private Double priceStringParser(String price){

        if(price != null) {
            price = price.replaceAll("[^\\d.]+", "");
            return Double.parseDouble(price);
        }
        return null;

    }

}


