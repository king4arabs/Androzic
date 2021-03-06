package com.androzic;

import com.androzic.data.Route;
import com.androzic.data.Waypoint;

public interface MapHolder
{
	MapView getMapView();
	void setFollowing(boolean follow);
	void zoomMap(float factor);
	/**
	 * Called when current map have changed
	 */
	void mapChanged();
	/**
	 * Called when location, zoom or other map conditions that need redraw have changed
	 */
	void conditionsChanged();
	void updateCoordinates(double[] latlon);
	void updateFileInfo();

	boolean waypointTapped(Waypoint waypoint, int x, int y);
	boolean routeWaypointTapped(Route route, int index, int x, int y);
	boolean mapObjectTapped(long id, int x, int y);
}
