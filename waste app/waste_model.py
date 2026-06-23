import numpy as np
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers

def create_model():
    # This is a simple CNN model for demonstration
    # In a real application, you would train this on waste classification datasets
    model = keras.Sequential([
        layers.Rescaling(1./255, input_shape=(180, 180, 3)),
        layers.Conv2D(16, 3, padding='same', activation='relu'),
        layers.MaxPooling2D(),
        layers.Conv2D(32, 3, padding='same', activation='relu'),
        layers.MaxPooling2D(),
        layers.Conv2D(64, 3, padding='same', activation='relu'),
        layers.MaxPooling2D(),
        layers.Flatten(),
        layers.Dense(128, activation='relu'),
        layers.Dense(4, activation='softmax')  # 4 classes: Organic, Recyclable, Hazardous, Mixed
    ])
    
    model.compile(
        optimizer='adam',
        loss='categorical_crossentropy',
        metrics=['accuracy']
    )
    
    return model

# This would be used to train the model in a real scenario
def train_model(model, train_data, validation_data):
    # Placeholder for training logic
    history = model.fit(
        train_data,
        validation_data=validation_data,
        epochs=10
    )
    return history

# This function would preprocess images for prediction
def preprocess_image(image):
    img = image.resize((180, 180))
    img_array = keras.utils.img_to_array(img)
    img_array = tf.expand_dims(img_array, 0)  # Create batch axis
    return img_array