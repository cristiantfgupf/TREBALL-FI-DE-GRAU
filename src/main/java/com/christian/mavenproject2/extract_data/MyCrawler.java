package com.christian.mavenproject2.extract_data;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

import javax.annotation.Priority;

import org.omg.CosNaming.NamingContextExtPackage.AddressHelper;

import com.christian.mavenproject2.analisy_algorithms.CreateMaps;
import com.christian.mavenproject2.analisy_algorithms.MyAlgorithms;
import com.christian.mavenproject2.crawler.HtmlParseData;
import com.christian.mavenproject2.crawler.Page;
import com.christian.mavenproject2.crawler.WebCrawler;
import com.christian.mavenproject2.crawler.WebURL;
import com.christian.mavenproject2.geoip.GeoIPv4;
import com.christian.mavenproject2.main.mainMenu;

public class MyCrawler extends WebCrawler {
	private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|mid|mp2|mp3|mp4|wav|avi|mov|mpeg|ram|m4v|pdf"
			+ "|rm|smil|wmv|swf|wma|zip|rar|gz|bmp|gif|jpe?g|png|tiff?))$");
	MyAlgorithms algorithms = new MyAlgorithms();

	/**
	 * This method receives two parameters. The first parameter is the page in
	 * which we have discovered this new url and the second parameter is the new
	 * url. You should implement this function to specify whether the given url
	 * should be crawled or not (based on your crawling logic). In this example,
	 * we are instructing the crawler to ignore urls that have css, js, git, ...
	 * extensions and to only accept urls that start with
	 * "http://www.ics.uci.edu/". In this case, we didn't need the referringPage
	 * parameter to make the decision.
	 */
	@Override
	public boolean shouldVisit(Page referringPage, WebURL url) {
		mainMenu menu = this.getMyController().menu;
		menu.enlacesTotales += 1;
		if(url.getDepth() == 0) return true;
		menu.setTextStats(menu.enlacesTotales + " ENLACES  |  " + menu.enlacesProcesados + " PROCESADOS  |  "
				+ menu.enlacesValidos + " VALIDOS  |  " + menu.enlacesAnalizados + " ANALIZADOS  |  "
				+ menu.enlacesErroneos + " ROTOS  |  " + menu.emailsFetched + " EMAILS");
		String href = url.getURL().toLowerCase();
		/*if (FILTERS.matcher(href).matches())
			return false;*/
		
		//if ((menu.linkIsContains || menu.linkIsNoContains) && !visitLinkCondition(menu, href)) return false;	
		
		if(menu.linkIsContains || menu.linkIsNoContains) if(!shouldVisitLink(url, menu, referringPage)) return false;
		if(menu.isPriority) applyPriority(url, referringPage, menu);
		return true;
	}
	
	/**
	 * This function is called when a page is fetched and ready to be processed
	 * by your program.
	 */
	@Override
	public void visit(Page page) {
		WebURL webURL = page.getWebURL();
		String geoURL = webURL.getSubDomain()+"."+webURL.getDomain();
		mainMenu menu = this.getMyController().menu;
		menu.enlacesProcesados += 1;
		menu.writeConsole("Procesando Página: " + page.getWebURL().getURL()+ "   " + page.getWebURL().getPriority() + "\n");
		if (page.getParseData() instanceof HtmlParseData && isUnderCondition(menu, page.getWebURL()) && visitLinkCondition(menu, page.getWebURL().getURL())
				&& algorithms.pageContainsContent(page, menu.contains, menu.isAll, menu.isAtLeast, menu.isNone, menu) && isGeolocation(geoURL,menu,menu.geoBoundingBox[0],menu.geoBoundingBox[1],menu.geoBoundingBox[2],menu.geoBoundingBox[3],page)) {
			menu.enlacesValidos += 1;
			menu.setTextStats(menu.enlacesTotales + " ENLACES  |  " + menu.enlacesProcesados + " PROCESADOS  |  "
					+ menu.enlacesValidos + " VALIDOS  |  " + menu.enlacesAnalizados + " ANALIZADOS  |  "
					+ menu.enlacesErroneos + " ROTOS  |  " + menu.emailsFetched + " EMAILS");
			menu.writeConsole(tiempoEjecucion(page, menu));
			menu.enlacesAnalizados += 1;
			menu.setTextStats(menu.enlacesTotales + " ENLACES  |  " + menu.enlacesProcesados + " PROCESADOS  |  "
					+ menu.enlacesValidos + " VALIDOS  |  " + menu.enlacesAnalizados + " ANALIZADOS  |  "
					+ menu.enlacesErroneos + " ROTOS  |  " + menu.emailsFetched + " EMAILS");
		}
	}

	public String removePrefix(String url) {
		if (url.startsWith("http://"))
			url = url.replace("http://", "");
		else if (url.startsWith("http:\\\\"))
			url = url.replace("http:\\\\", "");
		else if (url.startsWith("https://"))
			url = url.replace("https://", "");
		else if (url.startsWith("https:\\\\"))
			url = url.replace("https:\\\\", "");
		return url;
	}

	public Boolean isUnderCondition(mainMenu menu, WebURL href) {
		String s = href.getURL().toLowerCase();
		if (menu.isContiene) {
			boolean foundContiene = false;
			String[] contiene = menu.contiene;
			for (int i = 0; i < contiene.length; ++i) {
				if (s.contains(contiene[i]))
					foundContiene = true;
			}
			if (!foundContiene)
				return false;
		}

		if (menu.isNoContiene) {
			String[] noContiene = menu.noContiene;
			for (int i = 0; i < noContiene.length; ++i) {
				if (s.contains(noContiene[i]))
					return false;
			}
		}

		if (menu.isRegex) {
			Pattern regex = Pattern.compile(menu.regex);
			if (!regex.matcher(s).matches())
				return false;
		}
		return true;
	}

	public Boolean visitLinkCondition(mainMenu menu, String href) {
		String s = href.toLowerCase();
		if (menu.linkIsContains) {
			boolean foundContiene = false;
			String[] contiene = menu.linkContainsValue;
			for (int i = 0; i < contiene.length; ++i) {
				if (s.contains(contiene[i]))
					foundContiene = true;
			}
			if (!foundContiene)
				return false;
		}

		if (menu.linkIsNoContains) {
			String[] noContiene = menu.linkNoContainsValue;
			for (int i = 0; i < noContiene.length; ++i) {
				if (s.contains(noContiene[i]))
					return false;
			}
		}
		return true;
	}

	public String tiempoEjecucion(Page p, mainMenu menu) {
		CreateMaps cr = new CreateMaps();
		String s = "";
		String url = p.getWebURL().getURL().toLowerCase();
		String geoURL = p.getWebURL().getSubDomain()+"."+p.getWebURL().getDomain();
		s = "URL: " + url + "\n";
		String idioma = "";
		String email = "";
		String status = "";
		String geolocation = "";
		if (menu.isIdioma) {
			idioma = algorithms.detectLanguage(p);
			if (!idioma.isEmpty())
				s += "LANG: " + idioma + "\n";
		}
		if (menu.isEmails) {
			email = algorithms.getAllEmails(algorithms.detectEmails(p), menu);
			if (!email.isEmpty())
				s += "EMAIL: " + email + "\n";
		}
		if(menu.fetchGeolocation) {
			String[] geo = getGeolocation(geoURL);
			if(geo[2] != null) geolocation = geo[2] + "," + geo[0];
			else geolocation = geo[0];
			menu.heatMap += cr.addHeatMApValue(geo[8], geo[9]);
			menu.circleMap += cr.addCircleMApValue(geo[0], getIP(geoURL), geo[8], geo[9], menu.enlacesAnalizados, geoURL, url);
			s += "GEOLOCATION : " + geolocation +"\n";
		}
		// CountryName, CountryCode, City , PostalCode, Region, AreaCode, dmaCode, MetroCode, Latitude, Longitude
		menu.data.put(menu.enlacesAnalizados + 1 + "", new Object[] { url, idioma, email, geolocation });
		s += "\n";
		return s;
	}

	 public boolean isGeolocation(String url, mainMenu menu, float _lngSW, float _latSW, float _lngNE, float _latNE, Page page)
	 {
		 if(menu.isGeoLanguage)
		 {
			 MyAlgorithms myAlg = new MyAlgorithms();
			 if(!myAlg.detectLanguage(page).equals(menu.geoLanguage)) return false;
		 }
		 if(menu.isGeoBoundingBox)
		 {
			 String[] ipGeo = getGeolocation(url);
			float lng = (float)(Float.parseFloat(ipGeo[9]));
			float lat = (float)(Float.parseFloat(ipGeo[8]));
			if(!((lng >= _lngSW) && (lng <= _lngNE) && (lat >= _latSW) && (lat <= _latNE))) return false;
		 }
		 return true;
	 }

	// CountryName, CountryCode, City , PostalCode, Region, AreaCode, dmaCode, MetroCode, Latitude, Longitude
	public String[] getGeolocation(String url) {
		return GeoIPv4.getLocation(url).getALL();
	}
	
	public String getIP(String url)
	{
		InetAddress ip = null;
		try {
			ip = java.net.InetAddress.getByName(url);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ip.getHostAddress();
	}
	
	public void applyPriority(WebURL url,Page referringPage, mainMenu menu)
	{
		url.setPriority((byte)3);
		boolean found = false;
		if (menu.dataPriority < 0 && algorithms.pageContainsContent(referringPage, menu.contains, menu.isAll,
				menu.isAtLeast, menu.isNone, menu)) {
			found = true;
			if(menu.isPenalyze) {
				url.setPriority((byte)(url.getPriority()+menu.dataPriority+url.getDepth()));				
			}
			else url.setPriority((byte)(url.getPriority()+menu.dataPriority));			}
		
		if (menu.linkPriority < 0 && isUnderCondition(menu, url)) {
			found = true;
			if(menu.isPenalyze) {
				url.setPriority((byte)(url.getPriority()+menu.linkPriority+url.getDepth()));
			}
			else url.setPriority((byte)(url.getPriority()+menu.linkPriority));
		}
		if (!found) url.setPriority((byte)100);
	}
	
	public boolean contieneTerminos(String url, String[] contiene){
		for (int i = 0; i < contiene.length; ++i) {
			if (url.contains(contiene[i])) return true;
		}
		return false;
	}
	
	public boolean noContieneTerminos(String url, String[] noContiene){
		for (int i = 0; i < noContiene.length; ++i) {
			if (url.contains(noContiene[i]))
				return false;
		}
		return true;
	}
	
	
	public boolean shouldVisitLink(WebURL url, mainMenu menu, Page referringPage){
		String href = url.getURL().toLowerCase();
		if(menu.isBroken){
			if(url.getDepth() == 1 && !visitLinkCondition(menu, href)) return false;
			if(url.getDepth() > 1 && !visitLinkCondition(menu,referringPage.getWebURL().getURL()) && visitLinkCondition(menu,referringPage.getWebURL().getParentUrl())) return false;
		}
		else{
			if(!visitLinkCondition(menu, href)) return false;
		}
		return true;
	}
	
}