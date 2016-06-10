package org.crf.minutis;

public enum State {

	STARTING(2, R.string.state_starting,
	         R.drawable.ic_directions_walk_black_24dp,
	         R.drawable.ic_directions_walk_white_24dp),
	ON_SITE(3, R.string.state_on_site,
	        R.drawable.ic_place_black_24dp,
	        R.drawable.ic_place_white_24dp),
	EVACUATING(5, R.string.state_evacuating,
	           R.drawable.ic_ambulance_black_24dp,
	           R.drawable.ic_ambulance_white_24dp),
	ENDING(6, R.string.state_ending,
	       R.drawable.ic_local_hospital_black_24dp,
	       R.drawable.ic_local_hospital_white_24dp),
	LEAVING_HOSPITAL(4, R.string.state_leaving_hospital,
	                 R.drawable.ic_near_me_black_24dp,
	                 R.drawable.ic_near_me_white_24dp),
	AVAILABLE(7, R.string.state_available,
	          R.drawable.ic_account_check_black_24dp,
	          R.drawable.ic_account_check_white_24dp),
	BUSY(8, R.string.state_busy,
	     R.drawable.ic_account_remove_black_24dp,
	     R.drawable.ic_account_remove_white_24dp),
	HOME(9, R.string.state_home,
	     R.drawable.ic_home_black_24dp,
	     R.drawable.ic_home_white_24dp);

	public int code, icon, iconNotif, text;

	private State(int code, int text, int icon, int iconNotif) {
		this.code = code;
		this.text = text;
		this.icon = icon;
		this.iconNotif = iconNotif;
	}
}
