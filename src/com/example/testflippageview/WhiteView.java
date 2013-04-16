package com.example.testflippageview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;

class WhiteView extends View {
	public WhiteView(Context context) {
		super(context);
	}

	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawColor(Color.WHITE);
	}
}
