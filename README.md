# OnBoardingBubbles - Android
 [![](https://jitpack.io/v/alihaider63/OnBoardingBubbles.svg)](https://jitpack.io/#alihaider63/OnBoardingBubbles)
```groovy
dependencies {
    implementation 'com.github.alihaider63:OnBoardingBubbles:VERSION'
}
  ```

  <img src="resources/sample.gif" alt="GIF" height="700"/>
  
  ## How to use:
  
  ```kotlin
BubbleShowCaseBuilder(this) //Activity instance
                .title("foo") //Any title for the bubble view
                .description("bar") //More detailed description
                .arrowPosition(BubbleShowCase.ArrowPosition.RIGHT) //You can force the position of the arrow to change the location of the bubble.
                .backgroundColor(Color.GREEN) //Bubble background color
                .textColor(Color.BLACK) //Bubble Text color
                .titleTextSize(17) //Title text size in SP (default value 16sp)
                .descriptionTextSize(15) //Subtitle text size in SP (default value 14sp)
                .image(imageDrawable) //Bubble main image
                .closeActionImage(CloseImageDrawable) //Custom close action image
                .showOnce("BUBBLE_SHOW_CASE_ID") //Id to show only once the BubbleShowCase
                .listener(listener(object : BubbleShowCaseListener{ //Listener for user actions
                    override fun onTargetClick(bubbleShowCase: BubbleShowCase) {
                        //Called when the user clicks the target
                    }
                    override fun onCloseActionImageClick(bubbleShowCase: BubbleShowCase) {
                        //Called when the user clicks the close button
                    }
                    override fun onBubbleClick(bubbleShowCase: BubbleShowCase) {
                        //Called when the user clicks on the bubble
                    }

                    override fun onBackgroundDimClick(bubbleShowCase: BubbleShowCase) {
                        //Called when the user clicks on the background dim
                    }
                })
                .targetView(view) //View to point out
                .show() //Display the ShowCase
```
