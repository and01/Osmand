package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.osmo.OsMoGroupsActivity;
import net.osmand.plus.osmo.OsMoGroupsStorage.OsMoDevice;
import net.osmand.plus.osmo.OsMoPlugin;
import net.osmand.plus.osmo.OsMoPositionLayer;

public class OsMoMenuController extends MenuController {

	private OsMoDevice device;

	public OsMoMenuController(OsmandApplication app, MapActivity mapActivity, PointDescription pointDescription, final OsMoDevice device) {
		super(new MenuBuilder(app), pointDescription, mapActivity);
		this.device = device;

		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				if (OsMoPositionLayer.getFollowDestinationId() != null) {
					OsMoPositionLayer.setFollowDestination(null);
				} else {
					if(device.getLastLocation() != null) {
						TargetPointsHelper targets = getMapActivity().getMyApplication().getTargetPointsHelper();
						targets.navigateToPoint(new LatLon(device.getLastLocation().getLatitude(), device.getLastLocation().getLongitude()), true, -1);
					}
					OsMoPositionLayer.setFollowDestination(device);
				}
				getMapActivity().getContextMenu().updateMenuUI();
			}
		};

		rightTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				OsMoPlugin osMoPlugin = OsmandPlugin.getEnabledPlugin(OsMoPlugin.class);
				if (osMoPlugin != null) {
					OsMoGroupsActivity.showSettingsDialog(getMapActivity(), osMoPlugin, device);
				}
			}
		};
		rightTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_settings);
		rightTitleButtonController.leftIconId = R.drawable.ic_action_settings;

		updateData();
	}

	@Override
	public void updateData() {
		if (OsMoPositionLayer.getFollowDestinationId() != null) {
			leftTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_cancel);
			leftTitleButtonController.leftIconId = R.drawable.ic_action_remove_dark;
		} else {
			leftTitleButtonController.caption = getMapActivity().getString(R.string.mark_point);
			leftTitleButtonController.leftIconId = R.drawable.ic_action_flag_dark;
		}
	}

	@Override
	protected int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN;
	}

	@Override
	public boolean needTypeStr() {
		return true;
	}

	@Override
	public Drawable getLeftIcon() {
		if (isLight()) {
			return getIconOrig(R.drawable.widget_osmo_connected_location_day);
		} else {
			return getIconOrig(R.drawable.widget_osmo_connected_location_night);
		}
	}

	@Override
	public String getTypeStr() {
		OsmandApplication app = getMapActivity().getMyApplication();
		StringBuilder sb = new StringBuilder();
		final Location l = device.getLastLocation();
		if(l != null && l.hasSpeed()) {
			sb.append(OsmAndFormatter.getFormattedSpeed(l.getSpeed(), app));
			sb.append(" — ");
		}
		Location myLocation = app.getLocationProvider().getLastKnownLocation();
		if (myLocation != null) {
			float dist = myLocation.distanceTo(l);
			sb.append(OsmAndFormatter.getFormattedDistance(dist, app));
		}
		return sb.toString();
	}

	@Override
	public boolean needStreetName() {
		return false;
	}
}
