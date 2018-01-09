package mehdi.pdf;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by johndoe on 1/9/18.
 */

public class PDF extends View {

    private int mBackgroundRes;

    public PDF(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.PDF,
                0,
                0);

        try{
            mBackgroundRes = a.getInteger(R.styleable.PDF_background, R.drawable.pdf);
            setBackgroundResource(mBackgroundRes);
        } finally {
            a.recycle();
        }

    }


}
