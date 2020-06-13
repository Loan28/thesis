import numpy as np
import tensorflow as tf
import sys
from PIL import Image

args = sys.argv
file_name = args[1]
label_file = args[2]
model = args[3]
input_mean = 127.5
input_std = 127.5
def load_labels(filename):
    my_labels = []
    input_file = open(filename, 'r')
    for l in input_file:
        my_labels.append(l.strip())
    return my_labels

# Load the TFLite model and allocate tensors.
interpreter = tf.lite.Interpreter(model_path=model)
interpreter.allocate_tensors()

# Get input and output tensors.
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

height = input_details[0]['shape'][1]
width = input_details[0]['shape'][2]

img = Image.open(file_name)
img = img.resize((width, height))

# Test the model on random input data.
input_shape = input_details[0]['shape']
input_data = np.expand_dims(img, axis=0)
input_data = (np.float32(input_data) - input_mean) / input_std
interpreter.set_tensor(input_details[0]['index'], input_data)

interpreter.invoke()

# The function `get_tensor()` returns a copy of the tensor data.
# Use `tensor()` in order to get a pointer to the tensor.
output_data = interpreter.get_tensor(output_details[0]['index'])
results = np.squeeze(output_data)

labels = load_labels(label_file)
top_k = results.argsort()[-5:][::-1]
#for i in top_k:
#    if floating_model:
#        print('{0:08.6f}'.format(float(results[i]))+":", labels[i])
#    else:
#        print('{0:08.6f}'.format(float(results[i]/255.0))+":", labels[i])
print(labels[top_k[0]])