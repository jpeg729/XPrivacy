package biz.bokhorst.xprivacy;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class PackageChange extends BroadcastReceiver {
	@Override
	public void onReceive(final Context context, Intent intent) {
		try {
			// Check uri
			Uri inputUri = intent.getData();
			if (inputUri.getScheme().equals("package")) {
				// Get data
				int uid = intent.getIntExtra(Intent.EXTRA_UID, 0);
				boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
				NotificationManager notificationManager = (NotificationManager) context
						.getSystemService(Context.NOTIFICATION_SERVICE);

				Util.log(null, Log.WARN, "Package change action=" + intent.getAction() + " replacing=" + replacing
						+ " uid=" + uid);

				// Check action
				if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
					// Get data
					ApplicationInfoEx appInfo = new ApplicationInfoEx(context, uid);
					String packageName = inputUri.getSchemeSpecificPart();

					// Default deny new user apps
					if (PrivacyService.getClient() != null && appInfo.getPackageName().size() == 1) {
						if (!replacing) {
							// Delete existing restrictions
							PrivacyManager.deleteRestrictions(uid, null, false);
							PrivacyManager.deleteSettings(uid);
							PrivacyManager.deleteUsage(uid);

							boolean ondemand = PrivacyManager.getSettingBool(0, PrivacyManager.cSettingOnDemand, true,
									false);

							// Enable on demand
							if (ondemand)
								PrivacyManager.setSetting(uid, PrivacyManager.cSettingOnDemand, Boolean.toString(true));

							// Restrict new non-system apps
							if (!appInfo.isSystem())
								PrivacyManager.applyTemplate(uid, null, true);
						}
					}

					// Clear permissions cache
					synchronized (PrivacyManager.mPermissionRestrictionCache) {
						if (replacing && PrivacyManager.mPermissionRestrictionCache.get(uid) != null) {
							PrivacyManager.mPermissionRestrictionCache.remove(uid);
						}
					}

					// Mark as new/changed
					PrivacyManager.setSetting(uid, PrivacyManager.cSettingState,
							Integer.toString(ActivityMain.STATE_ATTENTION));

					// New/update notification
					boolean notify = PrivacyManager.getSettingBool(uid, PrivacyManager.cSettingNotify, true, false);
					if (!replacing || notify) {
						Intent resultIntent = new Intent(context, ActivityApp.class);
						resultIntent.putExtra(ActivityApp.cUid, uid);

						// Build pending intent
						PendingIntent pendingIntent = PendingIntent.getActivity(context, uid, resultIntent,
								PendingIntent.FLAG_UPDATE_CURRENT);

						// Build result intent settings
						Intent resultIntentSettings = new Intent(context, ActivityApp.class);
						resultIntentSettings.putExtra(ActivityApp.cUid, uid);
						resultIntentSettings.putExtra(ActivityApp.cAction, ActivityApp.cActionSettings);

						// Build pending intent settings
						PendingIntent pendingIntentSettings = PendingIntent.getActivity(context, uid - 10000,
								resultIntentSettings, PendingIntent.FLAG_UPDATE_CURRENT);

						// Build result intent clear
						Intent resultIntentClear = new Intent(context, ActivityApp.class);
						resultIntentClear.putExtra(ActivityApp.cUid, uid);
						resultIntentClear.putExtra(ActivityApp.cAction, ActivityApp.cActionClear);

						// Build pending intent clear
						PendingIntent pendingIntentClear = PendingIntent.getActivity(context, uid + 10000,
								resultIntentClear, PendingIntent.FLAG_UPDATE_CURRENT);

						// Title
						String title = String.format("%s %s %s",
								context.getString(replacing ? R.string.msg_update : R.string.msg_new),
								appInfo.getApplicationName(packageName),
								appInfo.getPackageVersionName(context, packageName));
						if (!replacing)
							title = String.format("%s %s", title, context.getString(R.string.msg_applied));

						// Build notification
						NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
						notificationBuilder.setSmallIcon(R.drawable.ic_launcher);
						notificationBuilder.setContentTitle(context.getString(R.string.app_name));
						notificationBuilder.setContentText(title);
						notificationBuilder.setContentIntent(pendingIntent);
						notificationBuilder.setWhen(System.currentTimeMillis());
						notificationBuilder.setAutoCancel(true);

						// Actions
						notificationBuilder.addAction(android.R.drawable.ic_menu_edit,
								context.getString(R.string.menu_app_settings), pendingIntentSettings);
						notificationBuilder.addAction(android.R.drawable.ic_menu_delete,
								context.getString(R.string.menu_clear), pendingIntentClear);

						// Notify
						Notification notification = notificationBuilder.build();
						notificationManager.notify(appInfo.getUid(), notification);
					}

				} else if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED) && !replacing) {
					// Package removed
					notificationManager.cancel(uid);

					// Delete restrictions
					ApplicationInfoEx appInfo = new ApplicationInfoEx(context, uid);
					if (PrivacyService.getClient() != null && appInfo.getPackageName().size() == 0) {
						PrivacyManager.deleteRestrictions(uid, null, false);
						PrivacyManager.deleteSettings(uid);
						PrivacyManager.deleteUsage(uid);
					}

				} else if (intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)) {
					// Notify reboot required
					String packageName = inputUri.getSchemeSpecificPart();
					if (packageName.equals(context.getPackageName())) {
						// Start package update
						Intent changeIntent = new Intent();
						changeIntent.setClass(context, UpdateService.class);
						changeIntent.putExtra(UpdateService.cAction, UpdateService.cActionUpdated);
						context.startService(changeIntent);

						// Build notification
						NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
						notificationBuilder.setSmallIcon(R.drawable.ic_launcher);
						notificationBuilder.setContentTitle(context.getString(R.string.app_name));
						notificationBuilder.setContentText(context.getString(R.string.msg_reboot));
						notificationBuilder.setWhen(System.currentTimeMillis());
						notificationBuilder.setAutoCancel(true);
						Notification notification = notificationBuilder.build();

						// Notify
						notificationManager.notify(Util.NOTIFY_RESTART, notification);
					}
				}
			}
		} catch (Throwable ex) {
			Util.bug(null, ex);
		}
	}
}
