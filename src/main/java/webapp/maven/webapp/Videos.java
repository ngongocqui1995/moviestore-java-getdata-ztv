package webapp.maven.webapp;

import java.util.ArrayList;

public class Videos {
	private String title;
	private String key;
	private ArrayList<Episodes> episodes;
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
	public ArrayList<Episodes> getEpisodes() {
		return episodes;
	}
	public void setEpisodes(ArrayList<Episodes> episodes) {
		this.episodes = episodes;
	}

}
