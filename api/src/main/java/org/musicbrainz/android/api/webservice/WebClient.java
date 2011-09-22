/*
 * Copyright (C) 2011 Jamie McDonald
 * 
 * This file is part of MusicBrainz Mobile (Android).
 * 
 * MusicBrainz Mobile (Android) is free software: you can redistribute 
 * it and/or modify it under the terms of the GNU General Public 
 * License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * MusicBrainz Mobile (Android) is distributed in the hope that it 
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied 
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MusicBrainz Mobile (Android). If not, see 
 * <http://www.gnu.org/licenses/>.
 */

package org.musicbrainz.android.api.webservice;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicResponseHandler;
import org.musicbrainz.android.api.data.Artist;
import org.musicbrainz.android.api.data.ArtistStub;
import org.musicbrainz.android.api.data.Release;
import org.musicbrainz.android.api.data.ReleaseGroupStub;
import org.musicbrainz.android.api.data.ReleaseStub;
import org.musicbrainz.android.api.data.UserData;

/**
 * Makes the web service available for Activity classes.
 * Calls are blocking and should be made inside AsyncTask.
 * XML returned is parsed into pojos with SAX.
 */
public class WebClient {
	
	private static final String AUTH_REALM = "musicbrainz.org";
	private static final String AUTH_SCOPE = "musicbrainz.org";
	private static final int AUTH_PORT = 80;
	private static final String AUTH_TYPE = "Digest";
	
	private AbstractHttpClient httpClient;
	private ResponseParser responseParser;
	
	private String clientId = "unknown";
	
	public WebClient() {
		httpClient = HttpClient.getClient();
		responseParser = new ResponseParser();
	}
	
	public WebClient(String username, String password, String appVersion) {
		httpClient = HttpClient.getClient();
		responseParser = new ResponseParser();
		setCredentials(username, password);
		clientId = appVersion;
	}
	
	public void setCredentials(String username, String password) {
		AuthScope authScope = new AuthScope(AUTH_SCOPE, AUTH_PORT, AUTH_REALM, AUTH_TYPE);
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
		httpClient.getCredentialsProvider().setCredentials(authScope, credentials);
	}
	
	public void setAppVersion(String appVersion) {
		clientId = appVersion;
	}
	
	public Release lookupReleaseFromBarcode(String barcode) throws IOException {
		HttpEntity entity = get(QueryBuilder.barcodeSearch(barcode));
		InputStream response = entity.getContent();
		String barcodeMbid = responseParser.parseMbidFromBarcode(response);
		entity.consumeContent();
		if (barcodeMbid == null) {
			throw new BarcodeNotFoundException(barcode);
		}
		return lookupRelease(barcodeMbid);
	}
	
	public Release lookupRelease(String mbid) throws IOException {
		HttpEntity entity = get(QueryBuilder.releaseLookup(mbid));
		InputStream response = entity.getContent();
		Release release = responseParser.parseRelease(response);
		entity.consumeContent();
		return release;
	}
	
	public LinkedList<ReleaseStub> browseReleases(String mbid) throws IOException {
		HttpEntity entity = get(QueryBuilder.releaseGroupReleaseBrowse(mbid));
		InputStream response = entity.getContent();
		LinkedList<ReleaseStub> releases = responseParser.parseReleaseGroupReleases(response);
		entity.consumeContent();
		return releases;
	}
	
	public Artist lookupArtist(String mbid) throws IOException {
		HttpEntity entity = get(QueryBuilder.artistLookup(mbid));
		InputStream artistResponse = entity.getContent();
		Artist artist = responseParser.parseArtist(artistResponse);
		entity.consumeContent();
		artist.setReleaseGroups(browseArtistReleaseGroups(mbid));
		return artist;
	}

	private ArrayList<ReleaseGroupStub> browseArtistReleaseGroups(String mbid) throws IOException {
		HttpEntity entity = get(QueryBuilder.artistReleaseGroupBrowse(mbid));
		InputStream rgResponse = entity.getContent();
		ArrayList<ReleaseGroupStub> releases = responseParser.parseReleaseGroupBrowse(rgResponse);
		entity.consumeContent();
		return releases;
	}
	
	public LinkedList<ArtistStub> searchArtists(String searchTerm) throws IOException {
		HttpEntity entity = get(QueryBuilder.artistSearch(searchTerm));
		InputStream response = entity.getContent();
		LinkedList<ArtistStub> artists = responseParser.parseArtistSearch(response);
		entity.consumeContent();
		return artists;
	}
	
	public LinkedList<ReleaseGroupStub> searchReleaseGroup(String searchTerm) throws IOException {
		HttpEntity entity = get(QueryBuilder.releaseGroupSearch(searchTerm));
		InputStream response = entity.getContent();
		LinkedList<ReleaseGroupStub> releaseGroups = responseParser.parseReleaseGroupSearch(response);
		entity.consumeContent();
		return releaseGroups;
	}
	
	public LinkedList<ReleaseStub> searchRelease(String searchTerm) throws IOException {
		HttpEntity entity = get(QueryBuilder.releaseSearch(searchTerm));
		InputStream response = entity.getContent();
		LinkedList<ReleaseStub> releases = responseParser.parseReleaseSearch(response);
		entity.consumeContent();
		return releases;
	}
	
	public Collection<String> lookupTags(MBEntity type, String mbid) throws IOException {
		HttpEntity entity = get(QueryBuilder.tagLookup(type, mbid));
		InputStream response = entity.getContent();
		Collection<String> tags = responseParser.parseTagLookup(response);
		entity.consumeContent();
		return tags;
	}
	
	public float lookupRating(MBEntity type, String mbid) throws IOException  {
		HttpEntity entity = get(QueryBuilder.ratingLookup(type, mbid));
		InputStream response = entity.getContent();
		float rating = responseParser.parseRatingLookup(response);
		entity.consumeContent();
		return rating;
	}
	
	protected HttpEntity get(String url) throws IOException {
		HttpGet get = new HttpGet(url);
		get.setHeader("Accept", "application/xml");
		HttpResponse response = httpClient.execute(get);
		return response.getEntity();
	}
	
	public boolean autenticateUserCredentials() throws IOException {
		HttpGet authenticationTest = new HttpGet(QueryBuilder.authenticationCheck());
		authenticationTest.setHeader("Accept", "application/xml");
		try {
			httpClient.execute(authenticationTest, new BasicResponseHandler());
		} catch (HttpResponseException e) {		
			return false;
		}
		return true;
	}
	
	public UserData getUserData(MBEntity entityType, String mbid) throws IOException {
		HttpEntity entity = get(QueryBuilder.userData(entityType, mbid));
		InputStream response = entity.getContent();
		UserData userData = responseParser.parseUserData(response);
		entity.consumeContent();
		return userData;
	}

	public void submitTags(MBEntity entityType, String mbid, Collection<String> tags) throws IOException {
		String url = QueryBuilder.tagSubmission(clientId);
		String content = XmlBuilder.buildTagSubmissionXML(entityType, mbid, tags);
		post(url, content);
	}
	
	public void submitRating(MBEntity entityType, String mbid, int rating) throws IOException {
		String url = QueryBuilder.ratingSubmission(clientId);
		String content = XmlBuilder.buildRatingSubmissionXML(entityType, mbid, rating);
		post(url, content);
	}
	
	public void submitBarcode(String mbid, String barcode) throws IOException {
		String url = QueryBuilder.barcodeSubmission(clientId);
		String content = XmlBuilder.buildBarcodeSubmissionXML(mbid, barcode);
		post(url, content);
	}
	
	private void post(String url, String content) throws IOException {
		HttpPost post = new HttpPost(url);
		post.addHeader("Content-Type", "application/xml; charset=UTF-8");
		StringEntity xml = new StringEntity(content, "UTF-8");
		post.setEntity(xml);
		HttpResponse response = httpClient.execute(post);
		if (response != null) {
			response.getEntity().consumeContent();
		}
	}
	
}
