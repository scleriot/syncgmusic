from flask import Flask, request
from gmusicapi import Webclient
import json

app = Flask(__name__)

@app.route('/list', methods=['GET', 'POST'])
def list():
	mm = Webclient()
	token = request.form['token']
	mm.setToken(token)
	songs = mm.get_all_songs(incremental=False)
	return json.dumps(songs)



@app.route('/playlists', methods=['GET', 'POST'])
def list():
	mm = Webclient()
	token = request.form['token']
	mm.setToken(token)
	playlists = mm.get_all_playlist_ids()
	output = "["

	songs = mm.get_all_songs(incremental=False)
	output += "{'title': 'Full library', 'songs' : "
	output += json.dumps(songs)
	output += "},"

	for (key,values) in playlists['user'].items():
		for playlistid in values:
			output += "{ 'title':'"+key+"', 'songs' :"
			songs=mm.get_playlist_songs(playlistid)
			output += json.dumps(songs)
			output += "},"

	output += "]"

	return output




@app.route('/download_song', methods=['GET', 'POST'])
def download_song():
	print "Request : Download url"
	mm = Webclient()
	token = request.form['token']
	songid = request.form['songid']
	mm.setToken(token)
	songs = mm.get_all_songs(incremental=False)
	url = mm.get_stream_url(songid)
	return url

@app.route('/login', methods=['GET', 'POST'])
def login():
	print "Request : Login"
	token = request.form['token']
	mm = Webclient()
	ok, token2 = mm.login_token(token)
	if ok :
		return token2


if __name__ == '__main__':
	app.debug = True
	app.run(host='0.0.0.0')