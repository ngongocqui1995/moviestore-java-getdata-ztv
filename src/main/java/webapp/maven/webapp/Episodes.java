package webapp.maven.webapp;

public class Episodes {
	private String title;
	private String url;
	private String urlReal;
	private String numberEpisodes;
	private String key;
	private String timeASet;
	private String img;
	
	public String getImg() {
		return img;
	}
	public void setImg(String img) {
		this.img = img;
	}
	public String getUrlReal() {
		return urlReal;
	}
	public void setUrlReal(String urlReal) {
		this.urlReal = urlReal;
	}
	public String getTitle() {
		return title;
	}
	public String getTimeASet() {
		return timeASet;
	}
	public void setTimeASet(String timeASet) {
		this.timeASet = timeASet;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getNumberEpisodes() {
		return numberEpisodes;
	}
	public void setNumberEpisodes(String numberEpisodes) {
		this.numberEpisodes = numberEpisodes;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
}
