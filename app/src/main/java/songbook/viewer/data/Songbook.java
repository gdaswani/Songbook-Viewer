package songbook.viewer.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Songbook {

	private long id;
	private String name;
	private String description;
	private boolean defaultFlag;

	private final List<Song> songs = new ArrayList<Song>();

	public Songbook(long id, String name, String description,
			boolean defaultFlag) {
		this.id = id;

		setName(name);
		setDescription(description);
		setDefaultFlag(defaultFlag);
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		if (name != null) {
			this.name = name.trim().toUpperCase();
		} else {
			this.name = null;
		}
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		if (description != null) {
			this.description = description.trim().toUpperCase();
		} else {
			this.description = null;
		}
	}

	public void addSong(Song aSong) {

		if (aSong == null) {
			throw new IllegalArgumentException("Cannot add song, invalid");
		}

		songs.add(aSong);
	}

	public String[] getTitleIndeces() {

		Set<String> titleIndeces = new TreeSet<String>();

		for (Song song : songs) {

			String title = song.getTitle();

			if (title != null && title.isEmpty() == false) {
				titleIndeces.add(String.valueOf(title.charAt(0)).toUpperCase());
			}
		}

		return titleIndeces.toArray(new String[0]);

	}

	public String[] getArtistIndeces() {

		Set<String> artistIndeces = new TreeSet<String>();

		for (Song song : songs) {

			String artist = song.getArtist();

			if (artist != null && artist.isEmpty() == false) {
				artistIndeces.add(String.valueOf(artist.charAt(0)));
			}
		}

		return artistIndeces.toArray(new String[0]);

	}

	public List<Song> getSongs() {
		return songs;
	}

	public boolean isDefaultFlag() {
		return defaultFlag;
	}

	public void setDefaultFlag(boolean defaultFlag) {
		this.defaultFlag = defaultFlag;
	}
}
