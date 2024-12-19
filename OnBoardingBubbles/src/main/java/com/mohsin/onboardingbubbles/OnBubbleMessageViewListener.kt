package com.mohsin.onboardingbubbles

interface OnBubbleMessageViewListener {
    /**
     * It is called when a user clicks the close action image in the BubbleMessageView
     */
    fun onCloseActionImageClick()


    /**
     * It is called when a user clicks the BubbleMessageView
     */
    fun onBubbleClick()

    /**
     * It is called when a user clicks the Next Button
     */
    fun onNextButtonClick()

    /**
     * It is called when a user clicks the label Button
     */
    fun onLabelButtonClick()
}