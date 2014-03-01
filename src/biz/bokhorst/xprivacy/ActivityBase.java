package biz.bokhorst.xprivacy;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

@SuppressLint("Registered")
public class ActivityBase extends Activity {
	private int mThemeId;
	private Bitmap[] mCheck = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (PrivacyService.checkClient()) {
			// Set theme
			String themeName = PrivacyManager.getSetting(0, PrivacyManager.cSettingTheme, "", false);
			mThemeId = (themeName.equals("Dark") ? R.style.CustomTheme : R.style.CustomTheme_Light);
			setTheme(mThemeId);
		} else {
			// Privacy client now available
			setContentView(R.layout.reboot);
			if (PrivacyService.getClient() == null) {
				TextView tvRebootClient = (TextView) findViewById(R.id.tvRebootClient);
				tvRebootClient.setVisibility(View.VISIBLE);
			} else {
				TextView tvRebootClient = (TextView) findViewById(R.id.tvRebootVersion);
				tvRebootClient.setVisibility(View.VISIBLE);
				Requirements.check(this);
			}
		}
	}

	protected Bitmap getOffCheckBox() {
		if (mCheck == null)
			buildCheckBoxes();
		return mCheck[0];
	}

	protected Bitmap getHalfCheckBox() {
		if (mCheck == null)
			buildCheckBoxes();
		return mCheck[1];
	}

	protected Bitmap getFullCheckBox() {
		if (mCheck == null)
			buildCheckBoxes();
		return mCheck[2];
	}

	protected Bitmap getOnDemandCheckBox() {
		if (mCheck == null)
			buildCheckBoxes();
		return mCheck[3];
	}

	protected Bitmap getCheckBoxImage(RState state) {
		Bitmap checkbox;

		if (state.partial)
			checkbox = getHalfCheckBox();
		else if (state.restricted)
			checkbox = getFullCheckBox();
		else
			checkbox = getOffCheckBox();

		return checkbox;
	}

	protected Bitmap getAskBoxImage(RState state) {
		Bitmap checkbox;

		if (state.asked)
			checkbox = getOffCheckBox();
		else
			checkbox = getOnDemandCheckBox();

		return checkbox;
	}

	protected Bitmap getFullCheckNoBox() {
		if (mCheck == null)
			buildCheckBoxes();
		return mCheck[4];
	}

	private void buildCheckBoxes() {
		mCheck = new Bitmap[5];

		// Get highlight color
		TypedArray ta1 = getTheme().obtainStyledAttributes(new int[] { android.R.attr.colorActivatedHighlight });
		int highlightColor = ta1.getColor(0, 0xFF00FF);
		ta1.recycle();

		// Get off check box
		TypedArray ta2 = getTheme().obtainStyledAttributes(new int[] { android.R.attr.listChoiceIndicatorMultiple });
		Drawable off = ta2.getDrawable(0);
		ta2.recycle();
		off.setBounds(0, 0, off.getIntrinsicWidth(), off.getIntrinsicHeight());

		// Get check mark
		Drawable checkmark = getResources().getDrawable(R.drawable.checkmark);
		checkmark.setBounds(0, 0, off.getIntrinsicWidth(), off.getIntrinsicHeight());
		checkmark.setColorFilter(highlightColor, Mode.SRC_ATOP);

		// Get check mark outline
		Drawable checkmarkOutline = getResources().getDrawable(R.drawable.checkmark_outline);
		checkmarkOutline.setBounds(0, 0, off.getIntrinsicWidth(), off.getIntrinsicHeight());

		// Create off check box
		mCheck[0] = Bitmap.createBitmap(off.getIntrinsicWidth(), off.getIntrinsicHeight(), Config.ARGB_8888);
		Canvas canvas0 = new Canvas(mCheck[0]);
		off.draw(canvas0);

		// Create half check box
		mCheck[1] = Bitmap.createBitmap(off.getIntrinsicWidth(), off.getIntrinsicHeight(), Config.ARGB_8888);
		Canvas canvas1 = new Canvas(mCheck[1]);
		off.draw(canvas1);
		Paint paint1 = new Paint();
		paint1.setStyle(Paint.Style.FILL);
		paint1.setColor(highlightColor);
		float wborder = off.getIntrinsicWidth() / 3f;
		float hborder = off.getIntrinsicHeight() / 3f;
		canvas1.drawRect(wborder, hborder, off.getIntrinsicWidth() - wborder, off.getIntrinsicHeight() - hborder,
				paint1);

		// Create full check box
		mCheck[2] = Bitmap.createBitmap(off.getIntrinsicWidth(), off.getIntrinsicHeight(), Config.ARGB_8888);
		Canvas canvas2 = new Canvas(mCheck[2]);
		off.draw(canvas2);
		checkmark.draw(canvas2);
		checkmarkOutline.draw(canvas2);

		// Get question mark
		Drawable questionmark = getResources().getDrawable(R.drawable.ondemand);
		questionmark.setBounds(0, 0, off.getIntrinsicWidth(), off.getIntrinsicHeight());
		questionmark.setColorFilter(highlightColor, Mode.SRC_ATOP);

		// Get question mark outline
		Drawable questionmarkOutline = getResources().getDrawable(R.drawable.questionmark_outline);
		questionmarkOutline.setBounds(0, 0, off.getIntrinsicWidth(), off.getIntrinsicHeight());

		// Create question check box
		mCheck[3] = Bitmap.createBitmap(off.getIntrinsicWidth(), off.getIntrinsicHeight(), Config.ARGB_8888);
		Canvas canvas3 = new Canvas(mCheck[3]);
		off.draw(canvas3);
		questionmark.draw(canvas3);
		questionmarkOutline.draw(canvas3);

		// Create full check without box
		mCheck[4] = Bitmap.createBitmap(off.getIntrinsicWidth(), off.getIntrinsicHeight(), Config.ARGB_8888);
		Canvas canvas4 = new Canvas(mCheck[4]);
		checkmark.setAlpha(170);
		checkmark.draw(canvas4);
		checkmarkOutline.setAlpha(170);
		checkmarkOutline.draw(canvas4);
	}

	public int getThemed(int attr) {
		TypedValue tv = new TypedValue();
		getTheme().resolveAttribute(attr, tv, true);
		return tv.resourceId;
	}
}
