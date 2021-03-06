/*
 * Androzic - android navigation client that uses OziExplorer maps (ozf2, ozfx3).
 * Copyright (C) 2010-2014 Andrey Novikov <http://andreynovikov.info/>
 * 
 * This file is part of Androzic application.
 * 
 * Androzic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Androzic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Androzic. If not, see <http://www.gnu.org/licenses/>.
 */

package com.androzic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.preference.PreferenceFragment;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.androzic.map.online.TileProvider;
import com.androzic.ui.SeekbarPreference;
import com.androzic.util.XmlUtils;

public class PreferencesHC extends ListFragment
{
	private FragmentHolder fragmentHolderCallback;

	private final ArrayList<Header> headers = new ArrayList<Header>();
	private HeaderAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		headers.clear();
		loadHeadersFromResource(R.xml.preference_headers, headers);
		adapter = new HeaderAdapter(getActivity(), headers);
		setListAdapter(adapter);

		/*
		 * Androzic application = (Androzic) getApplication();
		 * if (! application.isPaid)
		 * {
		 * for (int i = 0; i < getListAdapter().getCount(); i++)
		 * {
		 * if (R.id.pref_sharing == ((Header) getListAdapter().getItem(i)).id)
		 * {
		 * ((Header) getListAdapter().getItem(i)).summaryRes = R.string.donation_required;
		 * ((Header) getListAdapter().getItem(i)).fragmentArguments.putBoolean("disable", true);
		 * }
		 * }
		 * }
		 */

		/*
		 * if (getArguments().hasExtra("pref"))
		 * {
		 * for (int i = 0; i < getListAdapter().getCount(); i++)
		 * {
		 * if (getIntent().getIntExtra("pref", -1) == ((Header) getListAdapter().getItem(i)).id)
		 * {
		 * startWithFragment(((Header) getListAdapter().getItem(i)).fragment, ((Header) getListAdapter().getItem(i)).fragmentArguments, null, 0);
		 * finish();
		 * }
		 * }
		 * }
		 */
	}

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);

		// This makes sure that the container activity has implemented
		// the callback interface. If not, it throws an exception
		try
		{
			fragmentHolderCallback = (FragmentHolder) activity;
		}
		catch (ClassCastException e)
		{
			throw new ClassCastException(activity.toString() + " must implement FragmentHolder");
		}
	}

	@Override
	public void onListItemClick(ListView lv, View v, int position, long id)
	{
		Header header = adapter.getItem(position);
		
		Fragment fragment = Fragment.instantiate(getActivity(), header.fragment);
		Bundle args = header.fragmentArguments;
		if (args == null)
			args = new Bundle();
		//TODO We should use breadcrumbs or remove them from parser
		args.putString("title", (String) header.getTitle(getResources()));
		fragment.setArguments(args);
		
		fragmentHolderCallback.addFragment(fragment, header.fragment);
	}

	/**
	 * Parse the given XML file as a header description, adding each
	 * parsed Header into the target list.
	 *
	 * @param resid
	 *            The XML resource to load and parse.
	 * @param target
	 *            The list in which the parsed headers should be placed.
	 */
	public void loadHeadersFromResource(int resid, List<Header> target)
	{
		XmlResourceParser parser = null;
		try
		{
			parser = getResources().getXml(resid);
			AttributeSet attrs = Xml.asAttributeSet(parser);

			int type;
			while ((type = parser.next()) != XmlPullParser.END_DOCUMENT && type != XmlPullParser.START_TAG)
			{
				// Parse next until start tag is found
			}

			String nodeName = parser.getName();
			if (!"preference-headers".equals(nodeName))
			{
				throw new RuntimeException("XML document must start with <preference-headers> tag; found" + nodeName + " at " + parser.getPositionDescription());
			}

			Bundle curBundle = null;

			final int outerDepth = parser.getDepth();
			while ((type = parser.next()) != XmlPullParser.END_DOCUMENT && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth))
			{
				if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT)
				{
					continue;
				}

				nodeName = parser.getName();
				if ("header".equals(nodeName))
				{
					Header header = new Header();

					TypedArray sa = getResources().obtainAttributes(attrs, R.styleable.PreferenceHeader);
					header.id = sa.getResourceId(R.styleable.PreferenceHeader_id, (int) HEADER_ID_UNDEFINED);
					TypedValue tv = sa.peekValue(R.styleable.PreferenceHeader_title);
					if (tv != null && tv.type == TypedValue.TYPE_STRING)
					{
						if (tv.resourceId != 0)
						{
							header.titleRes = tv.resourceId;
						}
						else
						{
							header.title = tv.string;
						}
					}
					tv = sa.peekValue(R.styleable.PreferenceHeader_summary);
					if (tv != null && tv.type == TypedValue.TYPE_STRING)
					{
						if (tv.resourceId != 0)
						{
							header.summaryRes = tv.resourceId;
						}
						else
						{
							header.summary = tv.string;
						}
					}
					tv = sa.peekValue(R.styleable.PreferenceHeader_breadCrumbTitle);
					if (tv != null && tv.type == TypedValue.TYPE_STRING)
					{
						if (tv.resourceId != 0)
						{
							header.breadCrumbTitleRes = tv.resourceId;
						}
						else
						{
							header.breadCrumbTitle = tv.string;
						}
					}
					tv = sa.peekValue(R.styleable.PreferenceHeader_breadCrumbShortTitle);
					if (tv != null && tv.type == TypedValue.TYPE_STRING)
					{
						if (tv.resourceId != 0)
						{
							header.breadCrumbShortTitleRes = tv.resourceId;
						}
						else
						{
							header.breadCrumbShortTitle = tv.string;
						}
					}
					header.iconRes = sa.getResourceId(R.styleable.PreferenceHeader_icon, 0);
					header.fragment = sa.getString(R.styleable.PreferenceHeader_fragment);
					sa.recycle();

					if (curBundle == null)
					{
						curBundle = new Bundle();
					}

					final int innerDepth = parser.getDepth();
					while ((type = parser.next()) != XmlPullParser.END_DOCUMENT && (type != XmlPullParser.END_TAG || parser.getDepth() > innerDepth))
					{
						if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT)
						{
							continue;
						}

						String innerNodeName = parser.getName();
						if (innerNodeName.equals("extra"))
						{
							getResources().parseBundleExtra("extra", attrs, curBundle);
							XmlUtils.skipCurrentTag(parser);

						}
						else if (innerNodeName.equals("intent"))
						{
							header.intent = Intent.parseIntent(getResources(), parser, attrs);

						}
						else
						{
							XmlUtils.skipCurrentTag(parser);
						}
					}

					if (curBundle.size() > 0)
					{
						header.fragmentArguments = curBundle;
						curBundle = null;
					}

					target.add(header);
				}
				else
				{
					XmlUtils.skipCurrentTag(parser);
				}
			}

		}
		catch (XmlPullParserException e)
		{
			throw new RuntimeException("Error parsing headers", e);
		}
		catch (IOException e)
		{
			throw new RuntimeException("Error parsing headers", e);
		}
		finally
		{
			if (parser != null)
				parser.close();
		}

	}

	private static class HeaderAdapter extends ArrayAdapter<Header>
	{
		private static class HeaderViewHolder
		{
			ImageView icon;
			TextView title;
			TextView summary;
		}

		private LayoutInflater mInflater;

		public HeaderAdapter(Context context, List<Header> objects)
		{
			super(context, 0, objects);
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			HeaderViewHolder holder;
			View view;

			if (convertView == null)
			{
				view = mInflater.inflate(R.layout.preference_header_item, parent, false);
				holder = new HeaderViewHolder();
				holder.icon = (ImageView) view.findViewById(android.R.id.icon);
				holder.title = (TextView) view.findViewById(android.R.id.title);
				holder.summary = (TextView) view.findViewById(android.R.id.summary);
				view.setTag(holder);
			}
			else
			{
				view = convertView;
				holder = (HeaderViewHolder) view.getTag();
			}

			// All view fields must be updated every time, because the view may be recycled
			Header header = getItem(position);
			holder.icon.setImageResource(header.iconRes);
			holder.title.setText(header.getTitle(getContext().getResources()));
			CharSequence summary = header.getSummary(getContext().getResources());
			if (!TextUtils.isEmpty(summary))
			{
				holder.summary.setVisibility(View.VISIBLE);
				holder.summary.setText(summary);
			}
			else
			{
				holder.summary.setVisibility(View.GONE);
			}

			return view;
		}
	}

	/**
	 * Default value for {@link Header#id Header.id} indicating that no
	 * identifier value is set. All other values (including those below -1)
	 * are valid.
	 */
	public static final long HEADER_ID_UNDEFINED = -1;

	/**
	 * Description of a single Header item that the user can select.
	 */
	public static final class Header implements Parcelable
	{
		/**
		 * Identifier for this header, to correlate with a new list when
		 * it is updated. The default value is {@link PreferenceActivity#HEADER_ID_UNDEFINED}, meaning no id.
		 * 
		 * @attr ref android.R.styleable#PreferenceHeader_id
		 */
		public long id = HEADER_ID_UNDEFINED;

		/**
		 * Resource ID of title of the header that is shown to the user.
		 * 
		 * @attr ref android.R.styleable#PreferenceHeader_title
		 */
		public int titleRes;

		/**
		 * Title of the header that is shown to the user.
		 * 
		 * @attr ref android.R.styleable#PreferenceHeader_title
		 */
		public CharSequence title;

		/**
		 * Resource ID of optional summary describing what this header controls.
		 * 
		 * @attr ref android.R.styleable#PreferenceHeader_summary
		 */
		public int summaryRes;

		/**
		 * Optional summary describing what this header controls.
		 * 
		 * @attr ref android.R.styleable#PreferenceHeader_summary
		 */
		public CharSequence summary;

		/**
		 * Resource ID of optional text to show as the title in the bread crumb.
		 * 
		 * @attr ref android.R.styleable#PreferenceHeader_breadCrumbTitle
		 */
		public int breadCrumbTitleRes;

		/**
		 * Optional text to show as the title in the bread crumb.
		 * 
		 * @attr ref android.R.styleable#PreferenceHeader_breadCrumbTitle
		 */
		public CharSequence breadCrumbTitle;

		/**
		 * Resource ID of optional text to show as the short title in the bread crumb.
		 * 
		 * @attr ref android.R.styleable#PreferenceHeader_breadCrumbShortTitle
		 */
		public int breadCrumbShortTitleRes;

		/**
		 * Optional text to show as the short title in the bread crumb.
		 * 
		 * @attr ref android.R.styleable#PreferenceHeader_breadCrumbShortTitle
		 */
		public CharSequence breadCrumbShortTitle;

		/**
		 * Optional icon resource to show for this header.
		 * 
		 * @attr ref android.R.styleable#PreferenceHeader_icon
		 */
		public int iconRes;

		/**
		 * Full class name of the fragment to display when this header is
		 * selected.
		 * 
		 * @attr ref android.R.styleable#PreferenceHeader_fragment
		 */
		public String fragment;

		/**
		 * Optional arguments to supply to the fragment when it is
		 * instantiated.
		 */
		public Bundle fragmentArguments;

		/**
		 * Intent to launch when the preference is selected.
		 */
		public Intent intent;

		/**
		 * Optional additional data for use by subclasses of PreferenceActivity.
		 */
		public Bundle extras;

		public Header()
		{
			// Empty
		}

		/**
		 * Return the currently set title. If {@link #titleRes} is set,
		 * this resource is loaded from <var>res</var> and returned. Otherwise {@link #title} is returned.
		 */
		public CharSequence getTitle(Resources res)
		{
			if (titleRes != 0)
			{
				return res.getText(titleRes);
			}
			return title;
		}

		/**
		 * Return the currently set summary. If {@link #summaryRes} is set,
		 * this resource is loaded from <var>res</var> and returned. Otherwise {@link #summary} is returned.
		 */
		public CharSequence getSummary(Resources res)
		{
			if (summaryRes != 0)
			{
				return res.getText(summaryRes);
			}
			return summary;
		}

		/**
		 * Return the currently set bread crumb title. If {@link #breadCrumbTitleRes} is set,
		 * this resource is loaded from <var>res</var> and returned. Otherwise {@link #breadCrumbTitle} is returned.
		 */
		public CharSequence getBreadCrumbTitle(Resources res)
		{
			if (breadCrumbTitleRes != 0)
			{
				return res.getText(breadCrumbTitleRes);
			}
			return breadCrumbTitle;
		}

		/**
		 * Return the currently set bread crumb short title. If {@link #breadCrumbShortTitleRes} is set,
		 * this resource is loaded from <var>res</var> and returned. Otherwise {@link #breadCrumbShortTitle} is returned.
		 */
		public CharSequence getBreadCrumbShortTitle(Resources res)
		{
			if (breadCrumbShortTitleRes != 0)
			{
				return res.getText(breadCrumbShortTitleRes);
			}
			return breadCrumbShortTitle;
		}

		@Override
		public int describeContents()
		{
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags)
		{
			dest.writeLong(id);
			dest.writeInt(titleRes);
			TextUtils.writeToParcel(title, dest, flags);
			dest.writeInt(summaryRes);
			TextUtils.writeToParcel(summary, dest, flags);
			dest.writeInt(breadCrumbTitleRes);
			TextUtils.writeToParcel(breadCrumbTitle, dest, flags);
			dest.writeInt(breadCrumbShortTitleRes);
			TextUtils.writeToParcel(breadCrumbShortTitle, dest, flags);
			dest.writeInt(iconRes);
			dest.writeString(fragment);
			dest.writeBundle(fragmentArguments);
			if (intent != null)
			{
				dest.writeInt(1);
				intent.writeToParcel(dest, flags);
			}
			else
			{
				dest.writeInt(0);
			}
			dest.writeBundle(extras);
		}

		public void readFromParcel(Parcel in)
		{
			id = in.readLong();
			titleRes = in.readInt();
			title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
			summaryRes = in.readInt();
			summary = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
			breadCrumbTitleRes = in.readInt();
			breadCrumbTitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
			breadCrumbShortTitleRes = in.readInt();
			breadCrumbShortTitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
			iconRes = in.readInt();
			fragment = in.readString();
			fragmentArguments = in.readBundle();
			if (in.readInt() != 0)
			{
				intent = Intent.CREATOR.createFromParcel(in);
			}
			extras = in.readBundle();
		}

		Header(Parcel in)
		{
			readFromParcel(in);
		}

		public static final Creator<Header> CREATOR = new Creator<Header>() {
			public Header createFromParcel(Parcel source)
			{
				return new Header(source);
			}

			public Header[] newArray(int size)
			{
				return new Header[size];
			}
		};
	}

	public static class PreferencesFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener
	{
		@Override
		public void onCreate(Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);

			Bundle arguments = getArguments();

			if (arguments == null)
				return;

			String resource = arguments.getString("resource");
			if (resource != null)
			{
				int res = getActivity().getResources().getIdentifier(resource, "xml", getActivity().getPackageName());
				addPreferencesFromResource(res);
			}

			if (arguments.getBoolean("disable", false))
			{
				PreferenceScreen screen = getPreferenceScreen();
				for (int i = 0; i < screen.getPreferenceCount(); i++)
				{
					getPreferenceScreen().getPreference(i).setEnabled(false);
				}
			}
		}

		@Override
		public void onResume()
		{
			super.onResume();

			// initialize list summaries
			initSummaries(getPreferenceScreen());
			getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
			((ActionBarActivity) getActivity()).getSupportActionBar().setSubtitle(getArguments().getString("title"));
		}

		@Override
		public void onPause()
		{
			super.onPause();

			getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
			((ActionBarActivity) getActivity()).getSupportActionBar().setSubtitle(null);
		}

		@SuppressLint("NewApi")
		public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key)
		{
			if (key.equals(getString(R.string.pref_folder_root)))
			{
				Androzic application = (Androzic) getActivity().getApplication();
				String root = sharedPreferences.getString(key, Environment.getExternalStorageDirectory() + File.separator + getString(R.string.def_folder_prefix));
				application.setRootPath(root);
			}
			else if (key.equals(getString(R.string.pref_folder_map)))
			{
				final ProgressDialog pd = new ProgressDialog(getActivity());
				pd.setIndeterminate(true);
				pd.setMessage(getString(R.string.msg_initializingmaps));
				pd.show();

				new Thread(new Runnable() {
					public void run()
					{
						Androzic application = (Androzic) getActivity().getApplication();
						application.setMapPath(sharedPreferences.getString(key, getActivity().getResources().getString(R.string.def_folder_map)));
						pd.dismiss();
					}
				}).start();
			}
			else if (key.equals(getString(R.string.pref_charset)))
			{
				final ProgressDialog pd = new ProgressDialog(getActivity());
				pd.setIndeterminate(true);
				pd.setMessage(getString(R.string.msg_initializingmaps));
				pd.show();

				new Thread(new Runnable() {
					public void run()
					{
						Androzic application = (Androzic) getActivity().getApplication();
						application.charset = sharedPreferences.getString(key, "UTF-8");
						application.resetMaps();
						pd.dismiss();
					}
				}).start();
			}

			Preference pref = findPreference(key);
			setPrefSummary(pref);

			if (key.equals(getString(R.string.pref_onlinemap)))
			{
				Androzic application = (Androzic) getActivity().getApplication();
				SeekbarPreference mapzoom = (SeekbarPreference) findPreference(getString(R.string.pref_onlinemapscale));
				List<TileProvider> providers = application.getOnlineMaps();
				String current = sharedPreferences.getString(key, getResources().getString(R.string.def_onlinemap));
				TileProvider curProvider = null;
				for (TileProvider provider : providers)
				{
					if (current.equals(provider.code))
						curProvider = provider;
				}
				if (curProvider != null)
				{
					mapzoom.setMin(curProvider.minZoom);
					mapzoom.setMax(curProvider.maxZoom);
					int zoom = sharedPreferences.getInt(getString(R.string.pref_onlinemapscale), getResources().getInteger(R.integer.def_onlinemapscale));
					if (zoom < curProvider.minZoom)
					{
						SharedPreferences.Editor editor = sharedPreferences.edit();
						editor.putInt(getString(R.string.pref_onlinemapscale), curProvider.minZoom);
						editor.commit();

					}
					if (zoom > curProvider.maxZoom)
					{
						SharedPreferences.Editor editor = sharedPreferences.edit();
						editor.putInt(getString(R.string.pref_onlinemapscale), curProvider.maxZoom);
						editor.commit();
					}
				}
			}
			if (key.equals(getString(R.string.pref_locale)))
			{
				new AlertDialog.Builder(getActivity()).setTitle(R.string.restart_needed).setIcon(android.R.drawable.ic_dialog_alert).setMessage(getString(R.string.restart_needed_explained))
						.setCancelable(false).setPositiveButton(R.string.ok, null).show();
			}
			// TODO change intent name
			getActivity().sendBroadcast(new Intent("onSharedPreferenceChanged").putExtra("key", key));
			try
			{
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO)
					BackupManager.dataChanged("com.androzic");
			}
			catch (NoClassDefFoundError e)
			{
			}
		}

		private void setPrefSummary(Preference pref)
		{
			if (pref instanceof ListPreference)
			{
				CharSequence summary = ((ListPreference) pref).getEntry();
				if (summary != null)
				{
					pref.setSummary(summary);
				}
			}
			else if (pref instanceof EditTextPreference)
			{
				CharSequence summary = ((EditTextPreference) pref).getText();
				if (summary != null)
				{
					pref.setSummary(summary);
				}
			}
			else if (pref instanceof SeekbarPreference)
			{
				CharSequence summary = ((SeekbarPreference) pref).getText();
				if (summary != null)
				{
					pref.setSummary(summary);
				}
			}
		}

		private void initSummaries(PreferenceGroup preference)
		{
			for (int i = preference.getPreferenceCount() - 1; i >= 0; i--)
			{
				Preference pref = preference.getPreference(i);
				setPrefSummary(pref);

				if (pref instanceof PreferenceGroup || pref instanceof PreferenceScreen)
				{
					initSummaries((PreferenceGroup) pref);
				}
			}
		}
	}

	public static class PluginsPreferencesFragment extends PreferencesHC.PreferencesFragment
	{
		@Override
		public void onCreate(Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);

			PreferenceScreen root = getPreferenceManager().createPreferenceScreen(getActivity());
			root.setTitle(R.string.pref_plugins_title);
			setPreferenceScreen(root);

			Androzic application = (Androzic) getActivity().getApplication();
			Map<String, Intent> plugins = application.getPluginsPreferences();

			for (String plugin : plugins.keySet())
			{
				Preference preference = new Preference(getActivity());
				preference.setTitle(plugin);
				preference.setIntent(plugins.get(plugin));
				root.addPreference(preference);
			}
		}
	}

	public static class OnlineMapPreferencesFragment extends PreferencesHC.PreferencesFragment
	{
		@Override
		public void onResume()
		{
			Androzic application = (Androzic) getActivity().getApplication();

			ListPreference maps = (ListPreference) findPreference(getString(R.string.pref_onlinemap));
			SeekbarPreference mapzoom = (SeekbarPreference) findPreference(getString(R.string.pref_onlinemapscale));
			// initialize map list
			List<TileProvider> providers = application.getOnlineMaps();
			String[] entries = new String[providers.size()];
			String[] entryValues = new String[providers.size()];
			String current = getPreferenceScreen().getSharedPreferences().getString(getString(R.string.pref_onlinemap), getResources().getString(R.string.def_onlinemap));
			TileProvider curProvider = null;
			int i = 0;
			for (TileProvider provider : providers)
			{
				entries[i] = provider.name;
				entryValues[i] = provider.code;
				if (current.equals(provider.code))
					curProvider = provider;
				i++;
			}
			maps.setEntries(entries);
			maps.setEntryValues(entryValues);

			if (curProvider != null)
			{
				mapzoom.setMin(curProvider.minZoom);
				mapzoom.setMax(curProvider.maxZoom);
			}

			super.onResume();
		}
	}
}
