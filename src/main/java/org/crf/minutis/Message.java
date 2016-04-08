package org.crf.minutis;

public class Message {

	public enum Type {
		PERSON(R.drawable.ic_person_black_24dp),
		GROUP(R.drawable.ic_group_black_24dp);

		public final int image;

		Type(int image) {
			this.image = image;
		}
	}

	public String address;
	public String date;
	public String message;
	public Type type;


	public Message(Type type, String message, String date) {
		this(type, message, date, "");
	}
	public Message(Type type, String message, String date, String address) {
		this.type = type;
		this.message = message;
		this.date = date;
		this.address = address;
	}
}
