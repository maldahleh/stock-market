package com.maldahleh.stockmarket.stocks.provider;

import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.stocks.common.CacheableProvider;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

public class MarketStatusProvider extends CacheableProvider<Boolean> {

  public MarketStatusProvider(Settings settings) {
    super(settings);
  }

  @Override
  protected Boolean fetch(String key) {
    try {
      String requestXml =
          "<?xml version='1.0' encoding='utf−8'?><request devtype='Apple_OSX' "
              + "deployver='APPLE_DASHBOARD_1_0' app='YGoAppleStocksWidget' appver='unknown' "
              + "api='finance' apiver='1.0.1' acknotification='0000'><query id='0' timestamp='`"
              + "date +%s000`' type='getquotes'><list><symbol>"
              + key.toUpperCase()
              + "</symbol>"
              + "</list></query></request>";
      URL url = new URL("https://wu-quotes.apple.com/dgw?imei=42&apptype=finance");
      URLConnection con = url.openConnection();
      con.setDoInput(true);
      con.setDoOutput(true);
      con.setConnectTimeout(20_000);
      con.setReadTimeout(20_000);
      con.setUseCaches(false);
      con.setDefaultUseCaches(false);
      con.setRequestProperty("Content-Type", "text/xml");

      OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
      writer.write(requestXml);
      writer.flush();
      writer.close();

      InputStreamReader reader = new InputStreamReader(con.getInputStream());
      StringBuilder buf = new StringBuilder();
      char[] cbuf = new char[2048];
      int num;
      while (-1 != (num = reader.read(cbuf))) {
        buf.append(cbuf, 0, num);
      }

      reader.close();

      String finalResult = buf.toString();
      String[] initialSplit = finalResult.split("<status>");
      String[] finalSplit = initialSplit[1].split("</status>");

      return finalSplit[0].equalsIgnoreCase("1");
    } catch (Exception e) {
      return false;
    }
  }
}
