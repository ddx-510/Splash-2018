import textrazor

from flask import Flask, redirect, render_template, request, url_for, session, jsonify, make_response, abort


def classification(text):

    """news classification using text razor api"""

    textrazor.api_key = "2afab77eb63718df82c96d0669e0017cb0c6bcabb2c0ae4044fa58a7"
    client = textrazor.TextRazor(extractors=["entities","topics"])
    client.set_classifiers(["textrazor_newscodes"])
    response = client.analyze(text)

    categories = response.categories()
    category = categories[0].label

    return category

text = input("The news text to be analysed:\n")
print(classification(text))
