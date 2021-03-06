package net.osmand.plus.mapcontextmenu;

import android.support.v4.app.Fragment;
import android.view.View;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuController.MenuType;
import net.osmand.plus.mapcontextmenu.MenuController.TitleButtonController;
import net.osmand.plus.mapcontextmenu.MenuController.TitleProgressController;
import net.osmand.plus.mapcontextmenu.editors.FavoritePointEditor;
import net.osmand.plus.mapcontextmenu.editors.PointEditor;
import net.osmand.plus.mapcontextmenu.editors.WptPtEditor;
import net.osmand.plus.mapcontextmenu.other.MapMultiSelectionMenu;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.util.MapUtils;

import java.lang.ref.WeakReference;

public class MapContextMenu extends MenuTitleController {

	private MapActivity mapActivity;
	private MapMultiSelectionMenu mapMultiSelectionMenu;

	private FavoritePointEditor favoritePointEditor;
	private WptPtEditor wptPtEditor;

	private boolean active;
	private LatLon latLon;
	private PointDescription pointDescription;
	private Object object;
	private MenuController menuController;

	private LatLon mapCenter;
	private int mapPosition = 0;

	private LatLon myLocation;
	private Float heading;
	private boolean inLocationUpdate = false;

	private int favActionIconId;

	@Override
	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public void setMapActivity(MapActivity mapActivity) {
		this.mapActivity = mapActivity;

		if (mapMultiSelectionMenu == null) {
			mapMultiSelectionMenu = new MapMultiSelectionMenu(mapActivity);
		} else {
			mapMultiSelectionMenu.setMapActivity(mapActivity);
		}

		if (favoritePointEditor != null) {
			favoritePointEditor.setMapActivity(mapActivity);
		}
		if (wptPtEditor != null) {
			wptPtEditor.setMapActivity(mapActivity);
		}

		if (active) {
			acquireMenuController();
			if (menuController != null) {
				menuController.addPlainMenuItems(typeStr, this.pointDescription, this.latLon);
			}
		} else {
			menuController = null;
		}
	}

	public MapMultiSelectionMenu getMultiSelectionMenu() {
		return mapMultiSelectionMenu;
	}

	public boolean isActive() {
		return active;
	}

	public boolean isVisible() {
		return findMenuFragment() != null;
	}

	public FavoritePointEditor getFavoritePointEditor() {
		if (favoritePointEditor == null) {
			favoritePointEditor = new FavoritePointEditor(mapActivity);
		}
		return favoritePointEditor;
	}

	public WptPtEditor getWptPtPointEditor() {
		if (wptPtEditor == null) {
			wptPtEditor = new WptPtEditor(mapActivity);
		}
		return wptPtEditor;
	}

	public PointEditor getPointEditor(String tag) {
		if (favoritePointEditor != null && favoritePointEditor.getFragmentTag().equals(tag)) {
			return favoritePointEditor;
		} else if (wptPtEditor != null && wptPtEditor.getFragmentTag().equals(tag)) {
			return wptPtEditor;
		}
		return null;
	}

	@Override
	public LatLon getLatLon() {
		return latLon;
	}

	public LatLon getMapCenter() {
		return mapCenter;
	}

	public void setMapCenter(LatLon mapCenter) {
		this.mapCenter = mapCenter;
	}

	public void updateMapCenter(LatLon mapCenter) {
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().updateMapCenter(mapCenter);
		}
	}

	public void setMapPosition(int mapPosition) {
		this.mapPosition = mapPosition;
	}

	@Override
	public PointDescription getPointDescription() {
		return pointDescription;
	}

	@Override
	public Object getObject() {
		return object;
	}

	public boolean isExtended() {
		return menuController != null;
	}

	@Override
	public MenuController getMenuController() {
		return menuController;
	}

	public MapContextMenu() {
	}

	public boolean init(LatLon latLon, PointDescription pointDescription, Object object) {
		return init(latLon, pointDescription, object, false);
	}

	public boolean init(LatLon latLon, PointDescription pointDescription, Object object, boolean update) {

		if (myLocation == null) {
			myLocation = getMapActivity().getMyApplication().getSettings().getLastKnownMapLocation();
		}

		if (!update && isVisible()) {
			if (this.object == null || !this.object.equals(object)) {
				hide();
			} else {
				return false;
			}
		}

		setSelectedObject(object);

		if (pointDescription == null) {
			this.pointDescription = new PointDescription(latLon.getLatitude(), latLon.getLongitude());
		} else {
			this.pointDescription = pointDescription;
		}

		boolean needAcquireMenuController = menuController == null
				|| !update
				|| this.object == null && object != null
				|| this.object != null && object == null
				|| (this.object != null && object != null && !this.object.getClass().equals(object.getClass()));

		this.latLon = latLon;
		this.object = object;

		active = true;

		if (needAcquireMenuController) {
			acquireMenuController();
		}
		initTitle();

		if (menuController != null) {
			menuController.clearPlainMenuItems();
			menuController.addPlainMenuItems(typeStr, this.pointDescription, this.latLon);
		}

		if (mapPosition != 0) {
			mapActivity.getMapView().setMapPosition(0);
		}

		mapActivity.getMapView().refreshMap();

		return true;
	}

	public void show() {
		if (!isVisible()) {
			MapContextMenuFragment.showInstance(mapActivity);
		}
	}

	public void show(LatLon latLon, PointDescription pointDescription, Object object) {
		if (init(latLon, pointDescription, object)) {
			MapContextMenuFragment.showInstance(mapActivity);
		}
	}

	public void update(LatLon latLon, PointDescription pointDescription, Object object) {
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		init(latLon, pointDescription, object, true);
		if (fragmentRef != null) {
			fragmentRef.get().rebuildMenu();
		}
	}

	public void showOrUpdate(LatLon latLon, PointDescription pointDescription, Object object) {
		if (isVisible() && this.object != null && this.object.equals(object)) {
			update(latLon, pointDescription, object);
		} else {
			show(latLon, pointDescription, object);
		}
	}

	public void close() {
		active = false;
		if (this.object != null) {
			clearSelectedObject(this.object);
		}
		hide();
		mapActivity.getMapView().refreshMap();
	}

	public void hide() {
		if (mapPosition != 0) {
			mapActivity.getMapView().setMapPosition(mapPosition);
			mapPosition = 0;
		}
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().dismissMenu();
		}
	}

	public void updateMenuUI() {
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().updateMenu();
		}
	}

	private void clearSelectedObject(Object object) {
		if (object != null) {
			for (OsmandMapLayer l : mapActivity.getMapView().getLayers()) {
				if (l instanceof ContextMenuLayer.IContextMenuProvider) {
					PointDescription pointDescription = ((ContextMenuLayer.IContextMenuProvider) l).getObjectName(object);
					if (pointDescription != null) {
						if (l instanceof ContextMenuLayer.IContextMenuProviderSelection) {
							((ContextMenuLayer.IContextMenuProviderSelection) l).clearSelectedObject();
						}
					}
				}
			}
		}
	}

	private void setSelectedObject(Object object) {
		if (object != null) {
			for (OsmandMapLayer l : mapActivity.getMapView().getLayers()) {
				if (l instanceof ContextMenuLayer.IContextMenuProvider) {
					PointDescription pointDescription = ((ContextMenuLayer.IContextMenuProvider) l).getObjectName(object);
					if (pointDescription != null) {
						if (l instanceof ContextMenuLayer.IContextMenuProviderSelection) {
							((ContextMenuLayer.IContextMenuProviderSelection) l).setSelectedObject(object);
						}
					}
				}
			}
		}
	}

	private void acquireMenuController() {
		menuController = MenuController.getMenuController(mapActivity, pointDescription, object, MenuType.STANDARD);
	}

	public void onSingleTapOnMap() {
		if (menuController == null || !menuController.handleSingleTapOnMap()) {
			hide();
		}
	}

	@Override
	public void refreshMenuTitle() {
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null)
			fragmentRef.get().refreshTitle();
	}

	public WeakReference<MapContextMenuFragment> findMenuFragment() {
		Fragment fragment = mapActivity.getSupportFragmentManager().findFragmentByTag(MapContextMenuFragment.TAG);
		if (fragment != null && !fragment.isDetached()) {
			return new WeakReference<>((MapContextMenuFragment) fragment);
		} else {
			return null;
		}
	}

	public int getFavActionIconId() {
		return favActionIconId;
	}

	protected void acquireIcons() {
		super.acquireIcons();

		if (menuController != null) {
			favActionIconId = menuController.getFavActionIconId();
		} else {
			favActionIconId = R.drawable.ic_action_fav_dark;
		}
	}

	public void fabPressed() {
		mapActivity.getMapActions().showNavigationContextMenuPoint(latLon.getLatitude(), latLon.getLongitude());
		//mapActivity.getMapActions().directionTo(latLon.getLatitude(), latLon.getLongitude());
		hide();
		//mapActivity.getMapLayers().getMapControlsLayer().showRouteInfoControlDialog();
	}

	public void buttonWaypointPressed() {
		if (pointDescription.isDestination()) {
			mapActivity.getMapActions().editWaypoints();
		} else {
			mapActivity.getMapActions().addAsTarget(latLon.getLatitude(), latLon.getLongitude(),
					pointDescription);
		}
		close();
	}

	public void buttonFavoritePressed() {
		if (object != null && object instanceof FavouritePoint) {
			getFavoritePointEditor().edit((FavouritePoint) object);
		} else {
			getFavoritePointEditor().add(latLon, getTitleStr());
		}
	}

	public void buttonSharePressed() {
		if (menuController != null) {
			menuController.share(latLon, nameStr);
		} else {
			ShareMenu.show(latLon, nameStr, mapActivity);
		}
	}

	public void buttonMorePressed() {
		final ContextMenuAdapter menuAdapter = new ContextMenuAdapter(mapActivity);
		if (object != null) {
			for (OsmandMapLayer layer : mapActivity.getMapView().getLayers()) {
				layer.populateObjectContextMenu(object, menuAdapter);
			}
		}

		mapActivity.getMapActions().contextMenuPoint(latLon.getLatitude(), latLon.getLongitude(), menuAdapter, object);
	}

	public void addWptPt() {
		if (object == null || !(object instanceof WptPt)) {
			getWptPtPointEditor().add(latLon, getTitleStr());
		}
	}

	public void editWptPt() {
		if (object != null && object instanceof WptPt) {
			getWptPtPointEditor().edit((WptPt) object);
		}
	}

	public void setBaseFragmentVisibility(boolean visible) {
		WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().setFragmentVisibility(visible);
		}
	}

	public boolean isLandscapeLayout() {
		return menuController != null && menuController.isLandscapeLayout();
	}

	public float getLandscapeWidthDp() {
		if (menuController != null) {
			return menuController.getLandscapeWidthDp();
		} else {
			return 0f;
		}
	}

	public boolean slideUp() {
		return menuController != null && menuController.slideUp();
	}

	public boolean slideDown() {
		return menuController != null && menuController.slideDown();
	}

	public void build(View rootView) {
		if (menuController != null) {
			menuController.build(rootView);
		}
	}

	public int getCurrentMenuState() {
		if (menuController != null) {
			return menuController.getCurrentMenuState();
		} else {
			return MenuController.MenuState.HEADER_ONLY;
		}
	}

	public float getHalfScreenMaxHeightKoef() {
		if (menuController != null) {
			return menuController.getHalfScreenMaxHeightKoef();
		} else {
			return 0f;
		}
	}

	public int getSlideInAnimation() {
		if (menuController != null) {
			return menuController.getSlideInAnimation();
		} else {
			return 0;
		}
	}

	public int getSlideOutAnimation() {
		if (menuController != null) {
			return menuController.getSlideOutAnimation();
		} else {
			return 0;
		}
	}

	public TitleButtonController getLeftTitleButtonController() {
		if (menuController != null) {
			return menuController.getLeftTitleButtonController();
		} else {
			return null;
		}
	}

	public TitleButtonController getRightTitleButtonController() {
		if (menuController != null) {
			return menuController.getRightTitleButtonController();
		} else {
			return null;
		}
	}

	public TitleButtonController getTopRightTitleButtonController() {
		if (menuController != null) {
			return menuController.getTopRightTitleButtonController();
		} else {
			return null;
		}
	}

	public TitleProgressController getTitleProgressController() {
		if (menuController != null) {
			return menuController.getTitleProgressController();
		} else {
			return null;
		}
	}

	public boolean fabVisible() {
		return menuController == null || menuController.fabVisible();
	}

	public boolean buttonsVisible() {
		return menuController == null || menuController.buttonsVisible();
	}

	public boolean displayDistanceDirection() {
		return menuController != null && menuController.displayDistanceDirection();
	}

	public void updateData() {
		if (menuController != null) {
			menuController.updateData();
		}
	}

	public LatLon getMyLocation() {
		return myLocation;
	}

	public Float getHeading() {
		return heading;
	}

	public void updateMyLocation(net.osmand.Location location) {
		if (location != null && active && displayDistanceDirection()) {
			myLocation = new LatLon(location.getLatitude(), location.getLongitude());
			updateLocation(false, true, false);
		}
	}

	public void updateCompassValue(float value) {
		if (active && displayDistanceDirection()) {
			// 99 in next line used to one-time initialize arrows (with reference vs. fixed-north direction)
			// on non-compass devices
			float lastHeading = heading != null ? heading : 99;
			heading = value;
			if (Math.abs(MapUtils.degreesDiff(lastHeading, heading)) > 5) {
				updateLocation(false, false, true);
			} else {
				heading = lastHeading;
			}
		}
	}

	public void updateLocation(final boolean centerChanged, final boolean locationChanged,
							   final boolean compassChanged) {
		if (inLocationUpdate) {
			return;
		}
		inLocationUpdate = true;
		mapActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				inLocationUpdate = false;
				WeakReference<MapContextMenuFragment> fragmentRef = findMenuFragment();
				if (fragmentRef != null) {
					fragmentRef.get().updateLocation(centerChanged, locationChanged, compassChanged);
				}
			}
		});
	}

}