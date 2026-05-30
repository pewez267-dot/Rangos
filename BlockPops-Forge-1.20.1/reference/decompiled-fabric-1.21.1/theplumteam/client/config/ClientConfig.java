package com.theplumteam.client.config;

public class ClientConfig {
   private static ClientConfig instance;
   public float starColorR = 1.0F;
   public float starColorG = 1.0F;
   public float starColorB = 1.0F;
   public float starOpacity = 0.2F;
   public float backgroundColorR = 0.0F;
   public float backgroundColorG = 0.0F;
   public float backgroundColorB = 0.0F;
   public float panelOpacity = 0.9F;
   public boolean enableColorTransition = true;

   private ClientConfig() {
   }

   public static ClientConfig getInstance() {
      if (instance == null) {
         instance = new ClientConfig();
      }

      return instance;
   }

   public float[] getStarColor() {
      return new float[]{this.starColorR, this.starColorG, this.starColorB};
   }

   public void setStarColor(float r, float g, float b) {
      this.starColorR = Math.max(0.0F, Math.min(1.0F, r));
      this.starColorG = Math.max(0.0F, Math.min(1.0F, g));
      this.starColorB = Math.max(0.0F, Math.min(1.0F, b));
   }

   public float[] getBackgroundColor() {
      return new float[]{this.backgroundColorR, this.backgroundColorG, this.backgroundColorB};
   }

   public void setBackgroundColor(float r, float g, float b) {
      this.backgroundColorR = Math.max(0.0F, Math.min(1.0F, r));
      this.backgroundColorG = Math.max(0.0F, Math.min(1.0F, g));
      this.backgroundColorB = Math.max(0.0F, Math.min(1.0F, b));
   }

   public void resetColors() {
      this.starColorR = 1.0F;
      this.starColorG = 1.0F;
      this.starColorB = 1.0F;
      this.starOpacity = 0.2F;
      this.backgroundColorR = 0.0F;
      this.backgroundColorG = 0.0F;
      this.backgroundColorB = 0.0F;
      this.panelOpacity = 0.9F;
      this.enableColorTransition = true;
   }
}
