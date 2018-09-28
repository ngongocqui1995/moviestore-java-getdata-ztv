package webapp.maven.webapp;

import java.util.ArrayList;

public class Movie implements Comparable<Movie>{
	private String img;
	private String imgMain;
	private String title;
	private String key;
	private String content;
	private ArrayList<Countries> countries = new ArrayList<Countries>();
	private String timeASet;
	private ArrayList<Videos> videos = new ArrayList<Videos>();
	private ArrayList<Categories> categories = new ArrayList<Categories>();
	

	public String getImgMain() {
		return imgMain;
	}
	public void setImgMain(String imgMain) {
		this.imgMain = imgMain;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public ArrayList<Videos> getVideos() {
		return videos;
	}
	public void setVideos(ArrayList<Videos> videos) {
		this.videos = videos;
	}
	public ArrayList<Categories> getCategories() {
		return categories;
	}
	public void setCategories(ArrayList<Categories> categories) {
		this.categories = categories;
	}
	public ArrayList<Countries> getCountries() {
		return countries;
	}
	public void setCountries(ArrayList<Countries> countries) {
		this.countries = countries;
	}
	public String getTimeASet() {
		return timeASet;
	}
	public void setTimeASet(String timeASet) {
		this.timeASet = timeASet;
	}
	public String getImg() {
		return img;
	}
	public void setImg(String img) {
		this.img = img;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public int compareTo(Movie o) {
		return key.compareTo(o.key);
	}
	
}
