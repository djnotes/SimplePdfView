/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
modified by mehdi haghgoo
copyright © 2017 Kavosh Corporation
 */

package mehdi.pdf;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import java.io.File;
import java.io.IOException;


/**
 * This fragment has a big {@ImageView} that shows PDF pages, and 2 {@link android.widget.Button}s to move between
 * pages. We use a {@link android.graphics.pdf.PdfRenderer} to render PDF pages as {@link android.graphics.Bitmap}s.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)


public class PdfFragment extends Fragment implements View.OnClickListener{

    /**
     * Key string for saving the state of current page index.
     */
    private static final String STATE_CURRENT_PAGE_INDEX = "current_page_index";

    /**
     * The filename of the PDF.
     */

    private static final String TAG = PdfFragment.class.getSimpleName();

    /**
     * File descriptor of the PDF.
     */
    private ParcelFileDescriptor mFileDescriptor;

    /**
     * {@link android.graphics.pdf.PdfRenderer} to render the PDF.
     */
    private PdfRenderer mPdfRenderer;

    /**
     * Page that is currently shown on the screen.
     */
    private PdfRenderer.Page mCurrentPage;

    /**
     * {@link android.widget.ImageView} that shows a PDF page as a {@link android.graphics.Bitmap}
     */
    private ImageView mImageView;

    /**
     * {@link android.widget.Button} to move to the previous page.
     */
    private Button mButtonPrevious;

    /**
     * {@link android.widget.Button} to move to the next page.
     */
    private Button mButtonNext;
    private File mFile;

    /**
     * {@link android.widget.Button} to display current page number
     */
    private EditText mPageNumber;

    /**
     * {@link android.widget.TextView} to show total numbers
     */
    private TextView mTotalPages; //Displays total pages
    private GestureDetector mGestureDetector;

    /**
     * {@link float} swipe distance should be no less than this value to have effect
     */

    public PdfFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pdf, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Retain view references.
        mImageView = view.findViewById(R.id.image);
        mButtonPrevious = view.findViewById(R.id.previous);
        mButtonNext = view.findViewById(R.id.next);
        // Bind events.
        mButtonPrevious.setOnClickListener(this);
        mButtonNext.setOnClickListener(this);

        //Disable for back-next buttons for now
        mButtonPrevious.setEnabled(false);
        mButtonNext.setEnabled(false);

        //Initialize the two center views
        mPageNumber = view.findViewById(R.id.pageNumber);
        mTotalPages = view.findViewById(R.id.totalPages);

        // Show the first page by default.
        int index = 0;
        // If there is a savedInstanceState (screen orientations, etc.), we restore the page index.
        if (null != savedInstanceState) {
            index = savedInstanceState.getInt(STATE_CURRENT_PAGE_INDEX, 0);
            showPage(index);

        }
        mGestureDetector = new GestureDetector(getActivity(), new SideSwipeListener());
        mImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        });


    }


    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != mCurrentPage) {
            outState.putInt(STATE_CURRENT_PAGE_INDEX, mCurrentPage.getIndex());
        }
    }

    /**
     * Closes the {@link android.graphics.pdf.PdfRenderer} and related resources.
     *
     * @throws java.io.IOException When the PDF mFile cannot be closed.
     */
    private void closeRenderer() throws IOException {
        if (null != mCurrentPage) {
            mCurrentPage.close();
        }
        mPdfRenderer.close();
        mFileDescriptor.close();
    }

    /**
     * Shows the specified page of PDF to the screen.
     *
     * @param index The page index.
     */

    private void showPage(int index) {
        if (mPdfRenderer.getPageCount() <= index || index < 0) {
            return;
        }

        // Make sure to close the current page before opening another one.
        if (null != mCurrentPage) {
            mCurrentPage.close();
        }
        // Use `openPage` to open a specific page in PDF.
        try {
            mCurrentPage = mPdfRenderer.openPage(index);
        } catch (Exception e)
        {
            Log.e(TAG, "Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.an_error_occurred, Toast.LENGTH_LONG).show();
            return;
        }
        // Important: the destination bitmap must be ARGB (not RGB).
        Bitmap bitmap = Bitmap.createBitmap(mCurrentPage.getWidth(), mCurrentPage.getHeight(),
                Bitmap.Config.ARGB_8888);
        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get
        // the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        mCurrentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        // We are ready to show the Bitmap to user.
        mImageView.setImageBitmap(bitmap);
        updateUi();
    }

    /**
     * Updates the state of 2 control buttons in response to the current page index.
     */
    private void updateUi() {
        int index = mCurrentPage.getIndex();
        int pageCount = mPdfRenderer.getPageCount();
        mPageNumber.setText(String.format("%s", index + 1));
        mButtonPrevious.setEnabled(0 != index);
        mButtonNext.setEnabled(index + 1 < pageCount);
        Activity activity = getActivity();
        if (activity != null) {
            //activity.setTitle(getString(R.string.app_name_with_index, index + 1, pageCount));
        }
    }

    /**
     * Gets the number of pages in the PDF. This method is marked as public for testing.
     *
     * @return The number of pages.
     */
    public int getPageCount() {
        return mPdfRenderer.getPageCount();
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.previous) {
            showPage(mCurrentPage.getIndex() - 1);
        } else if (i == R.id.next) {
            showPage(mCurrentPage.getIndex() + 1);
        }
    }

    /**
     * Loads the mFile received as input and initializes mPdfRenderer
     *
     * @param file
     */
    public void loadDocument(File file) {
        try {
            mFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            // This is the PdfRenderer we use to render the PDF.
            mPdfRenderer = new PdfRenderer(mFileDescriptor);
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;// IMPORTANT
        }
        showPage(0);
        mTotalPages.setText(String.valueOf(mPdfRenderer.getPageCount()));
    }

    public void goNext() {
        showPage(mCurrentPage.getIndex() + 1);
    }

    public void goPrev() {
        showPage(mCurrentPage.getIndex() - 1);
    }

    private class SideSwipeListener implements GestureDetector.OnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true; //If you return false, the system assumes you don't want the rest of the gesture
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            //Calculate distance
            float x1 = e1.getX();
            float x2 = e2.getX();

            float distanceX = x2 - x1;

            if(distanceX > 0 ) {
                goPrev();
                //Swipe left should display right pages
            }
            else if (distanceX < 0) {
                //Swipe right should bring up pages from the left
                goNext();
            }

            return true;
        }
    }

}
