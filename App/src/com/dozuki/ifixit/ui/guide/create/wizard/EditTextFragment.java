/*
 * Copyright 2012 Roman Nurik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dozuki.ifixit.ui.guide.create.wizard;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.dozuki.ifixit.R;
import com.dozuki.ifixit.model.guide.wizard.EditTextPage;
import com.dozuki.ifixit.ui.BaseFragment;
import com.dozuki.ifixit.util.Utils;

public class EditTextFragment extends BaseFragment {
   private static final String ARG_KEY = "key";

   private PageFragmentCallbacks mCallbacks;
   private String mKey;
   private EditTextPage mPage;
   private EditText mField;

   public static EditTextFragment create(String key) {
      Bundle args = new Bundle();
      args.putString(ARG_KEY, key);

      EditTextFragment fragment = new EditTextFragment();
      fragment.setArguments(args);
      return fragment;
   }

   public EditTextFragment() { }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      Bundle args = getArguments();
      mKey = args.getString(ARG_KEY);
      if (mKey == null && savedInstanceState != null) {
         mKey = savedInstanceState.getString(ARG_KEY);
      }
   }

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container,
    Bundle savedInstanceState) {
      View rootView = inflater.inflate(R.layout.guide_create_intro_edit_text, container, false);

      mPage = (EditTextPage) mCallbacks.onGetPage(mKey);

      ((TextView) rootView.findViewById(android.R.id.title)).setText(mPage.getTitle());
      ((TextView) rootView.findViewById(R.id.page_description)).setText(mPage.getDescription());

      mField = (EditText) rootView.findViewById(R.id.edit_text_field);
      mField.setText(mPage.getData().getString(EditTextPage.TEXT_DATA_KEY));
      mField.setHint(mPage.getHint());

      return rootView;

   }

   @Override
   public void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);

      outState.putString(ARG_KEY, mKey);
   }

   @Override
   public void onAttach(Activity activity) {
      super.onAttach(activity);

      if (!(activity instanceof PageFragmentCallbacks)) {
         throw new ClassCastException("Activity must implement PageFragmentCallbacks");
      }

      mCallbacks = (PageFragmentCallbacks) activity;
   }

   @Override
   public void onDetach() {
      super.onDetach();
      mCallbacks = null;
   }

   @Override
   public void onViewCreated(View view, Bundle savedInstanceState) {
      super.onViewCreated(view, savedInstanceState);

      mField.addTextChangedListener(new TextWatcher() {
         @Override
         public void beforeTextChanged(CharSequence charSequence, int i, int i1,
          int i2) { }

         @Override
         public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

         @Override
         public void afterTextChanged(Editable editable) {
            if (mPage.shouldStripNewlines()) {
               editable = Utils.stripNewlines(editable);
            }

            mPage.getData().putString(EditTextPage.TEXT_DATA_KEY,
             (editable != null) ? editable.toString() : null);
            mPage.notifyDataChanged();
         }
      });

   }

   @Override
   public void setMenuVisibility(boolean menuVisible) {
      super.setMenuVisibility(menuVisible);

      // In a future update to the support library, this should override setUserVisibleHint
      // instead of setMenuVisibility.
      if (mField != null) {
         InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
          Context.INPUT_METHOD_SERVICE);
         if (!menuVisible) {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
         }
      }
   }
}
