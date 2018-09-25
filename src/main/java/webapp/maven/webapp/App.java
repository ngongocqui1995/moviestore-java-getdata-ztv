package webapp.maven.webapp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Hello world!
 *
 */
public class App 
{
	private final static String USER_AGENT = "Mozilla/5.0";
	
    public static void main( String[] args ) throws IOException, InterruptedException
    {    	
    	// lấy tất cả thể loại
    	ArrayList<String> category = getAllCategory("http://localhost:2900/collectiontv/categories");
    	
    	// lấy tất cả danh phim của các thể loại
    	ArrayList<Movie> dsMovies = getAllListMovie("http://localhost:2900/collectiontv/", category);
    	
    	category.clear();
    	
    	// lấy tất cả thông tin của phim
    	ArrayList<Movie> dsInfomartionMovie = getAllListInfomartionMovie("http://localhost:2900/collectiontvinformation/", dsMovies);
    	
    	dsMovies.clear();
    	
    	// lấy tất cả tập phim trong 1 bộ phim
    	ArrayList<Movie> dsAllMovie = getAllVideosOfMovie("http://localhost:2900/collectiontv/", dsInfomartionMovie);
    	
    	dsInfomartionMovie.clear();
    	
    	// kiểm tra có lấy được video không ?
    	ArrayList<Movie> dsAllMovieChecked = checkVideoOfMovie(dsAllMovie);
    	
    	dsAllMovie.clear();
    	
    	printTxt(dsAllMovieChecked);
    	
    	ArrayList<Movie> dsAllMovieUploadImage = uploadImageOfMovie(dsAllMovieChecked);
    	
    	dsAllMovieChecked.clear();
    	    	
    	printTxt(dsAllMovieUploadImage);
    	
    	for(int i=0; i<dsAllMovieUploadImage.size(); i++) {
    		sendDataServer("http://localhost:3004/collection", dsAllMovieUploadImage.get(i));
    	}
    }

	private static ArrayList<Movie> uploadImageOfMovie(ArrayList<Movie> dsAllMovieChecked) throws IOException, InterruptedException {
		ArrayList<Movie> dsMovie = new ArrayList<Movie>(dsAllMovieChecked);
		for(int i=0; i<dsMovie.size(); i++) {
			String keyOfMovie = dsMovie.get(i).getKey();
			String urlImageOfMovie = dsMovie.get(i).getImg();
			String imgOfMovie = uploadImage("http://localhost:3004/dowloadImage", urlImageOfMovie, keyOfMovie);
			dsMovie.get(i).setImg(imgOfMovie);
			
			
			String urlImageMainOfMovie = dsMovie.get(i).getImgMain();
			String imgMainOfMovie = uploadImage("http://localhost:3004/dowloadImage", urlImageMainOfMovie, keyOfMovie);
			dsMovie.get(i).setImgMain(imgMainOfMovie);
			
			
			ArrayList<Videos> dsVideos = new ArrayList<Videos>(dsMovie.get(i).getVideos());
			for(int j=0; j<dsVideos.size(); j++) {
				ArrayList<Episodes> dsEpisodes = new ArrayList<Episodes>(dsVideos.get(j).getEpisodes());
				for(int k=0; k<dsEpisodes.size(); k++) {
					String urlImageOfEpisodes = dsEpisodes.get(k).getImg();
					String imgOfEpisodes = uploadImage("http://localhost:3004/dowloadImage", urlImageOfEpisodes, keyOfMovie);
					dsMovie.get(i).getVideos().get(j).getEpisodes().get(k).setImg(imgOfEpisodes);
				}
			}
		}

		return dsMovie;
	}

	private static ArrayList<Movie> checkVideoOfMovie(ArrayList<Movie> dsAllMovie) throws IOException, InterruptedException {
		ArrayList<Movie> dsMovie = new ArrayList<Movie>(dsAllMovie);
		ArrayList<Integer> indexRemoveVideos = new ArrayList<Integer>();
		ArrayList<Integer> indexRemoveMovies = new ArrayList<Integer>();
		for(int i=0; i<dsMovie.size(); i++) {
			ArrayList<Videos> videos = new ArrayList<Videos>(dsMovie.get(i).getVideos());
			for(int j=0; j<videos.size(); j++) {
				ArrayList<Episodes> episodes = new ArrayList<Episodes>(videos.get(j).getEpisodes());
				if(episodes.size() > 0) {
					boolean kt = checkVideo("http://localhost:2900/collectiontv/video", episodes.get(0).getUrlReal());
					if(!kt) indexRemoveVideos.add(j);
				}else if(episodes.size() == 0){
					indexRemoveVideos.add(j);
				}
			}
			
			for(int k=0; k<indexRemoveVideos.size(); k++) {
				dsMovie.get(i).getVideos().remove(indexRemoveVideos.get(k)-k);
			}
			indexRemoveVideos.clear();
			if(dsMovie.get(i).getVideos().size() == 0) indexRemoveMovies.add(i);
		}
		
		for(int i=0; i<indexRemoveMovies.size(); i++) {
			dsMovie.remove(indexRemoveMovies.get(i)-i);
		}
		
		return dsMovie;
	}
	
	private static Boolean checkVideo(String url, String paramsUrl) throws IOException, InterruptedException {
		Boolean kt = false;	
		URL obj = new URL(url);
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put("url", paramsUrl);
		System.out.println(paramsUrl);
		
		StringBuilder postData = new StringBuilder();
		for(Map.Entry<String, Object> param : params.entrySet()) {
			if(postData.length() != 0 ) postData.append("&");
			postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
			postData.append("=");
			postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
		}
		byte[] postDataBytes = postData.toString().getBytes("UTF-8");
		
		HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setDoOutput(true);
		conn.getOutputStream().write(postDataBytes);
		
		
		Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
		
		StringBuilder sb = new StringBuilder();
		for(int c; (c = in.read()) >= 0;) sb.append((char)c);
		in.close();
		

		String response = sb.toString();
		
		JSONArray json = new JSONArray(response.toString());
		if(json.length() > 0) {
			kt = true;
		}
		System.out.println(kt);
		
		return kt;
	}

	private static void sendDataServer(String url, Movie movie) throws IOException, InterruptedException {
		URL obj = new URL(url);
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put("title", movie.getTitle());
		params.put("key", movie.getKey());
		params.put("countries", new JSONArray(movie.getCountries()).toString());
		params.put("content", movie.getContent());
		params.put("categories", new JSONArray(movie.getCategories()).toString());
		params.put("img", movie.getImg());
		params.put("imgMain", movie.getImgMain());
		params.put("timeASet", movie.getTimeASet());
		params.put("videos", new JSONArray(movie.getVideos()).toString());
		
		StringBuilder postData = new StringBuilder();
		for(Map.Entry<String, Object> param : params.entrySet()) {
			if(postData.length() != 0 ) postData.append("&");
			postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
			postData.append("=");
			postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
		}
		byte[] postDataBytes = postData.toString().getBytes("UTF-8");
		
		HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setDoOutput(true);
		conn.getOutputStream().write(postDataBytes);
		
		
		Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
		
		StringBuilder sb = new StringBuilder();
		for(int c; (c = in.read()) >= 0;) sb.append((char)c);
		in.close();
		

		String response = sb.toString();
		
		if(response == "{ message: 'update ok' }") {
			printResultUpdate(movie.getKey());
		}
		System.out.println(response);
	}

	private static void printResultUpdate(String key) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter("file/resultUpdate.txt", true));
		bw.write(key);
		bw.newLine();
		bw.close();
	}

	private static void printTxt(ArrayList<Movie> dsAllMovie) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter("file/json.txt"));
		bw.write(new JSONArray(dsAllMovie).toString(2));
		bw.close();
	}

	private static ArrayList<Movie> getAllVideosOfMovie(String string, ArrayList<Movie> dsInfomartionMovie) throws IOException, InterruptedException {
		ArrayList<Movie> dsMovie = new ArrayList<Movie>(dsInfomartionMovie);
		ArrayList<Integer> indexRemove = new ArrayList<Integer>();
		for(int i=0; i<dsMovie.size(); i++) {
			Document doc = Jsoup.connect("https://tv.zing.vn/"+dsMovie.get(i).getKey()).get();
			Element divClassMidWrap =  doc.getElementsByClass("mid-wrap").get(0);
			Elements divClassTitleBar = divClassMidWrap.getElementsByClass("title-bar");
			int lenght = divClassTitleBar.size();
			
			ArrayList<Videos> dsVideos = new ArrayList<Videos>();
			for(int j=0; j<lenght; j++) {
				Videos videos = new Videos();
				int sl = divClassTitleBar.get(j).children().size();
				if(sl == 2 && divClassTitleBar.get(j).child(1).getElementsByTag("a").get(0).attr("href").indexOf("html") == -1 ) {				
					String title = divClassTitleBar.get(j).child(0).text().trim();
					String key = Normalizer.normalize(title, Normalizer.Form.NFD);
		    	    Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
		    	    key = pattern.matcher(key).replaceAll("").toLowerCase().replaceAll("đ", "d");
		    	    key = convertString(key);
		    	    
		    	    // lấy tất cả tập phim trong 1 bộ phim
		    	    String url = divClassTitleBar.get(j).child(1).getElementsByTag("a").get(0).attr("href");
		    	    ArrayList<Episodes> dsEpisodes = getAllEpisodesOfMovie("http://localhost:2900/collectiontv"+url);
		    	    
		    	    
		    	    videos.setTitle(title);
		    	    videos.setKey(key);
		    	    videos.setEpisodes(dsEpisodes);
				}else if(sl == 1) {			
					String title = divClassTitleBar.get(j).child(0).text().trim();
					String key = Normalizer.normalize(title, Normalizer.Form.NFD);
					Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
					key = pattern.matcher(key).replaceAll("").toLowerCase().replaceAll("đ", "d");
		    	    key = convertString(key);
		    	    
		    	    // lấy tất cả tập phim trong 1 bộ phim
		    	    ArrayList<Episodes> dsEpisodes = getAllEpisodesOfMovie("http://localhost:2900/collectiontvepisodes/"+dsMovie.get(i).getKey()+"/"+j);
		    	    
		    	    
		    	    videos.setTitle(title);
		    	    videos.setKey(key);
		    	    videos.setEpisodes(dsEpisodes);
				}
				if(sl == 1 || sl == 2 && divClassTitleBar.get(j).child(1).getElementsByTag("a").get(0).attr("href").indexOf("html") == -1) {
					dsVideos.add(videos);
				}
			}
			dsMovie.get(i).setVideos(dsVideos);
			
			if(dsMovie.get(i).getVideos().size() == 0) {
				indexRemove.add(i);
			}
		}
		
		for(int i=0; i<indexRemove.size(); i++) {
			dsMovie.remove(indexRemove.get(i)-i);
		}
		
		return dsMovie;
	}

    private static ArrayList<Episodes> getAllEpisodesOfMovie(String url) throws IOException, InterruptedException {
    	URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		
		con.setRequestMethod("GET");
		
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.connect();
		
		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);
		
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
		
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		con.disconnect();

		JSONArray result = new JSONArray(response.toString());
		ArrayList<Episodes> dsEpisodes = new ArrayList<Episodes>();
    	for(int j=0; j<result.length(); j++) {
    		JSONObject object = (JSONObject) result.get(j);
    		
    		Episodes episodes = new Episodes();
    		episodes.setTitle(object.get("title").toString());
    		String key = Normalizer.normalize(object.get("title").toString(), Normalizer.Form.NFD);
			Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
			key = pattern.matcher(key).replaceAll("").toLowerCase().replaceAll("đ", "d");
			key = convertString(key);
    		episodes.setKey(key);
    		episodes.setUrlReal("https://tv.zing.vn"+object.get("link").toString());
    		episodes.setUrl(md5("https://tv.zing.vn"+object.get("link").toString()));
    		episodes.setNumberEpisodes(object.get("episodes").toString());
    		episodes.setTimeASet(object.get("timeASet").toString());
    		
    		// up ảnh lên server
    		String img = object.get("img").toString();
    		episodes.setImg(img);
    		
    		dsEpisodes.add(episodes);
    	}
		return dsEpisodes;
	}
    
    private static String convertString(String str) {
    	String result = str.trim();
    	while (result.indexOf("…") != -1) result = result.replaceAll("…"," ").trim();
    	while (result.indexOf(",") != -1) result = result.replaceAll("\\,"," ").trim();
    	while (result.indexOf("<") != -1) result = result.replaceAll("\\<"," ").trim();
    	while (result.indexOf(".") != -1) result = result.replaceAll("\\."," ").trim();
    	while (result.indexOf(">") != -1) result = result.replaceAll("\\>"," ").trim();
    	while (result.indexOf("/") != -1) result = result.replaceAll("\\/"," ").trim();
    	while (result.indexOf("?") != -1) result = result.replaceAll("\\?"," ").trim();
    	while (result.indexOf(";") != -1) result = result.replaceAll("\\;"," ").trim();
    	while (result.indexOf(":") != -1) result = result.replaceAll("\\:"," ").trim();
    	while (result.indexOf("'") != -1) result = result.replaceAll("\\'"," ").trim();
    	while (result.indexOf("\"") != -1) result = result.replaceAll("\\\""," ").trim();
    	while (result.indexOf("[") != -1) result = result.replaceAll("\\["," ").trim();
    	while (result.indexOf("]") != -1) result = result.replaceAll("\\]"," ").trim();
    	while (result.indexOf("{") != -1) result = result.replaceAll("\\{"," ").trim();
    	while (result.indexOf("}") != -1) result = result.replaceAll("\\}"," ").trim();
    	while (result.indexOf("|") != -1) result = result.replaceAll("\\|"," ").trim();
    	while (result.indexOf("\\") != -1) result = result.replaceAll("\\"," ").trim();
    	while (result.indexOf("+") != -1) result = result.replaceAll("\\+"," ").trim();
    	while (result.indexOf("=") != -1) result = result.replaceAll("\\="," ").trim();
    	while (result.indexOf("-") != -1) result = result.replaceAll("\\-"," ").trim();
    	while (result.indexOf("_") != -1) result = result.replaceAll("\\_"," ").trim();
    	while (result.indexOf("(") != -1) result = result.replaceAll("\\("," ").trim();
    	while (result.indexOf(")") != -1) result = result.replaceAll("\\)"," ").trim();
    	while (result.indexOf("*") != -1) result = result.replaceAll("\\*"," ").trim();
    	while (result.indexOf("&") != -1) result = result.replaceAll("\\&"," ").trim();
    	while (result.indexOf("^") != -1) result = result.replaceAll("\\^"," ").trim();
    	while (result.indexOf("%") != -1) result = result.replaceAll("\\%"," ").trim();
    	while (result.indexOf("$") != -1) result = result.replaceAll("\\$"," ").trim();
    	while (result.indexOf("#") != -1) result = result.replaceAll("\\#"," ").trim();
    	while (result.indexOf("@") != -1) result = result.replaceAll("\\@"," ").trim();
    	while (result.indexOf("!") != -1) result = result.replaceAll("\\!"," ").trim();
    	while (result.indexOf("`") != -1) result = result.replaceAll("\\`"," ").trim();
    	while (result.indexOf("~") != -1) result = result.replaceAll("\\~"," ").trim();
    	while (result.indexOf("+") != -1) result = result.replaceAll("\\+"," ").trim();
    	while (result.indexOf("  ") != -1) result = result.replaceAll("  "," ").trim();
    	while (result.indexOf(" ") != -1) result = result.replaceAll(" ","-").trim();
    	return result;
    }
    
    private static String md5(String str) {
    	String result = "";
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("MD5");
			digest.update(str.getBytes());
			BigInteger bigInteger = new BigInteger(1,digest.digest());
			result = bigInteger.toString(16);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return result;
    }

	private static ArrayList<Movie> getAllListInfomartionMovie(String url, ArrayList<Movie> dsMovies) throws IOException, InterruptedException {
		ArrayList<Movie> dsMovie = new ArrayList<Movie>(dsMovies);
		int thoat = 0;
		ArrayList<Integer> indexRemove = new ArrayList<Integer>();
		for(int i=0; i<dsMovie.size(); i++) {
			JSONArray result;
			do {
				URL obj = new URL(url+dsMovie.get(i).getKey());
				HttpURLConnection con = (HttpURLConnection) obj.openConnection();
				
				con.setRequestMethod("GET");
				
				con.setRequestProperty("User-Agent", USER_AGENT);
				con.connect();
				
				int responseCode = con.getResponseCode();
				System.out.println("\nSending 'GET' request to URL : " + url+dsMovie.get(i).getKey());
				System.out.println("Response Code : " + responseCode);
				
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();
				
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();
				con.disconnect();
	
				result = new JSONArray(response.toString());
				thoat++;
			}while(result.length() == 0 && thoat <= 10);
			if(result.length() == 0) indexRemove.add(i);
			
	    	for(int j=0; j<result.length(); j++) {
	    		JSONObject object = (JSONObject) result.get(j);
	    		
	    		// lấy thông tin từng phim
	    		dsMovie.get(i).setContent(object.get("description").toString());
	    		
	    		dsMovie.get(i).setImgMain(object.get("imgMain").toString());
	    		
	    		// lấy quốc gia
			    ArrayList<Countries> dsCountries = new ArrayList<Countries>();
	    		Countries countries = new Countries();
	    		countries.setTitle(object.get("country").toString());
	    		String keyCountries = Normalizer.normalize(object.get("country").toString(), Normalizer.Form.NFD);
				Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
				keyCountries = pattern.matcher(keyCountries).replaceAll("").toLowerCase().replaceAll("đ", "d");
			    keyCountries = convertString(keyCountries);
			    countries.setKey(keyCountries);
			    
			    dsCountries.add(countries);
	    		dsMovie.get(i).setCountries(dsCountries);
	    		
	    		dsMovie.get(i).setTimeASet(object.get("timeASet").toString());	
	    		
	    		// lấy thông tin thể loại
				JSONArray jsonCategories = new JSONArray(object.get("categories").toString());
				ArrayList<Categories> dsCategories = new ArrayList<Categories>();
				for(int k=0; k<jsonCategories.length(); k++) {
		    		JSONObject objectCategories = (JSONObject) jsonCategories.get(k);
		    		
		    		Categories categories = new Categories();
		    		categories.setTitle(objectCategories.get("title").toString());
		    		String keyCategories = Normalizer.normalize(objectCategories.get("title").toString(), Normalizer.Form.NFD);
					Pattern pattern1 = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
					keyCategories = pattern1.matcher(keyCategories).replaceAll("").toLowerCase().replaceAll("đ", "d");
		    		categories.setKey(convertString(keyCategories));
		    		
		    		dsCategories.add(categories);
				}
				dsMovie.get(i).setCategories(dsCategories);
	    	}
		}
		
		// xoá những phim không có thông tin
		for(int i=0; i<indexRemove.size(); i++) {
			dsMovie.remove(indexRemove.get(i)-i);
		}
		
		return dsMovie;
	}

	private static ArrayList<Movie> getAllListMovie(String url, ArrayList<String> category) throws IOException, InterruptedException {
		ArrayList<Movie> dsMovie = new ArrayList<Movie>();
		
		for(int i=0; i<category.size(); i++) {
			int page = getNumberPage(url+"page/"+category.get(i));
			
			
			for(int j=1; j<=page; j++) {
				URL obj = new URL(url+category.get(i)+"/"+j);
				HttpURLConnection con = (HttpURLConnection) obj.openConnection();
				
				con.setRequestMethod("GET");
				
				con.setRequestProperty("User-Agent", USER_AGENT);
				con.connect();
				
				int responseCode = con.getResponseCode();
				System.out.println("\nSending 'GET' request to URL : " + url+category.get(i)+"/"+j);
				System.out.println("Response Code : " + responseCode);
				
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();
				
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();
				con.disconnect();
				
				
		    	JSONArray result = new JSONArray(response.toString());
		    	for(int k=0; k<result.length(); k++) {
		    		JSONObject object = (JSONObject) result.get(k);
		    		
		    		String key = object.get("key").toString().trim();
		    		boolean kt = checkExitsMovie(key, dsMovie);
		    		if(!kt) {
			    		// lấy thông tin từng phim
			    		Movie movie = new Movie();
			    		movie.setTitle(object.get("title").toString());
			    		
			    		// up ảnh lên server
			    		String img = object.get("img").toString();
			    		movie.setImg(img);
			    		movie.setKey(key);
			    		
						dsMovie.add(movie);
		    		}
		    	}
			}
		}
		
		return dsMovie;
	}
	
	

	private static boolean checkExitsMovie(String key, ArrayList<Movie> dsMovie) {
		boolean kt = false;
		for(Movie movie:dsMovie) {
			if(movie.getKey() == key) {
				kt = true;
				break;
			}
		}
		return kt;
	}

	private static String uploadImage(String url, String urlImage, String file) throws IOException, InterruptedException {
		URL obj = new URL(url);
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put("url", urlImage);
		params.put("file", file);
		System.out.println(urlImage);
		
		StringBuilder postData = new StringBuilder();
		for(Map.Entry<String, Object> param : params.entrySet()) {
			if(postData.length() != 0 ) postData.append("&");
			postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
			postData.append("=");
			postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
		}
		byte[] postDataBytes = postData.toString().getBytes("UTF-8");
		
		HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
		
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setDoOutput(true);
		conn.getOutputStream().write(postDataBytes);
		
		Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
		
		StringBuilder sb = new StringBuilder();
		for(int c; (c = in.read()) >= 0;) sb.append((char)c);
		in.close();
		

		String response = sb.toString();
		
		JSONObject json = new JSONObject(response);
		String img = (String) json.get("img"); 
		
		return img;
	}

	private static int getNumberPage(String url) throws IOException, InterruptedException {
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		
		con.setRequestMethod("GET");
		
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.connect();
		
		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);
		
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
		
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		con.disconnect();

    	JSONArray result = new JSONArray(response.toString());
		JSONObject object = (JSONObject) result.get(0);
		int page = (Integer) object.get("page");

		return page;
	}

	private static ArrayList<String> getAllCategory(String url) throws IOException, InterruptedException {
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		
		con.setRequestMethod("GET");
		
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.connect();
		
		int responseCode = con.getResponseCode();

		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);
		
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
		
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		con.disconnect();
		
		ArrayList<String> category = new ArrayList<String>();
    	JSONArray result = new JSONArray(response.toString());
    	for(int i=0; i<result.length(); i++) {
    		JSONObject object = (JSONObject) result.get(i);
    		
    		// lấy items trong result
    		JSONArray items = new JSONArray(object.get("items").toString());
    		for(int j=0; j<items.length(); j++) {
    			JSONObject objectItem = (JSONObject) items.get(j);
    			String key = objectItem.get("key").toString();
    			
    			category.add(key);
    		}
    	}
    	return category;
	}
}
