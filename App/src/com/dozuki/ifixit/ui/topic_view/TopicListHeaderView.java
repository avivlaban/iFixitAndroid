package com.dozuki.ifixit.ui.topic_view;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.dozuki.ifixit.R;

public class TopicListHeaderView extends LinearLayout {
   private TextView mTopicName;

   public TopicListHeaderView(Context context) {
      super(context);

      LayoutInflater inflater = (LayoutInflater)context.getSystemService(
       Context.LAYOUT_INFLATER_SERVICE);
      inflater.inflate(R.layout.topic_header_row, this, true);

      mTopicName = (TextView)findViewById(R.id.topic_header);
   }

   public void setHeader(String header) {
      mTopicName.setText(header);
   }
}
