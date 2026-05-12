package org.thoughtcrime.securesms.util;

import androidx.annotation.StyleRes;
import org.thoughtcrime.securesms.R;

public class DynamicNoActionBarTheme extends DynamicTheme {
  protected @StyleRes int getLightThemeStyle() {
    return R.style.Theme_DeltaChat;
  }

  protected @StyleRes int getDarkThemeStyle() {
    return R.style.Theme_DeltaChat;
  }
}
