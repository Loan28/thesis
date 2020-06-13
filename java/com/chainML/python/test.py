import tensorflow as tf
import tensorflow_hub as hub
from matplotlib import pyplot as plt
import numpy as np
import sys
from skimage import data
from PIL import Image


module = hub.KerasLayer("/home/loan/IdeaProjects/chainML/python/resnet_model/")
args = sys.argv
im = tf.io.read_file(args[1])
im = tf.image.decode_jpeg(im, channels=3) #color images
im = tf.image.convert_image_dtype(im, tf.float32)
#convert unit8 tensor to floats in the [0,1]range
t = tf.image.resize(im, [224, 224])

images = np.array([t])
images = tf.cast(images, tf.float32)
images = images/255

logits = module(images)  # Logits with shape [batch_size, 1000].
probabilities = tf.nn.softmax(logits)

print(np.argmax(probabilities))
