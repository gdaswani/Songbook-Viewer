package songbook.viewer.data;

public class Song {

	private long id;
	private long sbId;
	private String artist;
	private String title;
	private int numCode;

	public Song(long id, long sbId, String title, String artist, int numCode) {
		super();
		this.id = id;
		this.sbId = sbId;
		setTitle(title);
		setArtist(artist);
		setNumCode(numCode);
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		if (artist != null) {
			this.artist = artist.trim().toUpperCase();
		} else {
			this.artist = null;
		}
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		if (title != null) {
			this.title = title.trim().toUpperCase();
		} else {
			this.title = null;

		}
	}

	public int getNumCode() {
		return numCode;
	}

	public void setNumCode(int numCode) {
		this.numCode = numCode;
	}

	public long getSbId() {
		return sbId;
	}

	public void setSbId(long sbId) {
		this.sbId = sbId;
	}
}
