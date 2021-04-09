package com.example.animation.fresco;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class SmartImageViewPostprocessorsWrapper {
    private static boolean useFresco = true;

    public static void updateWithExternal(@NonNull SmartImageView.External external) {
        useFresco = external.getUseFresco();
    }

    @Nullable
    public static SmartImageViewPostprocessor wrap(SmartImageViewPostprocessor postprocessor) {
        if (postprocessor == null) {
            return null;
        }
        if (useFresco) {
            return new SmartImageViewPostprocessorFrescoWrapper(postprocessor);
        }
        return postprocessor;
    }
}
