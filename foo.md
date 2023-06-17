# Scala coding challenge

As a consultancy company Hivemind builds software based on customer requirements.
Imagine the description below to be the requirements of a customer project. Before handing over the code to the customer
we usually do code reviews. Therefore, be ready to guide us through your solution. You should also make sure that your
solution meets production quality standards.
You are free to define your own standards but please make sure that you can explain them.

Parts of this exercise and its requirements are unclear, left out or ambiguous on purpose.  
Some reasons are:

- Most projects have unclear requirements
- We want to see how you tackle that and also want to allow a variety of solutions

## Amazon reviews

Amazons online-shop is well known for its review system. Customers can
write [reviews](https://www.amazon.com/Legend-Zelda-Links-Awakening-game-boy/dp/B00002ST3U?th=1#customerReviews) for
products and rate them with stars from 1 to 5. Customer reviews themselves can be rated as helpful or not helpful by
other customers.

Your task is to create a web service that provides an API that takes certain kind of requests and uses the reviews to
provide helpful responses.

## The data

Fortunately, you don't have to crawl Amazon for the review data. The data are provided in the form of a newline
delimited JSON file, each line containing one JSON object that describes a customer review for a product. The number of
reviews is not known in advance but its structure is always the same and looks like that:

```json
{
  "asin": "B000Q75VCO",
  "helpful": [
    16,
    40
  ],
  "overall": 2.0,
  "reviewText": "Words are in my not-so-humble opinion, the most inexhaustible form of magic we have, capable both of inflicting injury and remedying it.",
  "reviewerID": "B07844AAA04E4",
  "reviewerName": "Gaylord Bashirian",
  "summary": "Ut deserunt adipisci aut.",
  "unixReviewTime": 1475261866
}
{
  "asin": "B000NI7RW8",
  "helpful": [
    32,
    52
  ],
  "overall": 3.0,
  "reviewText": "Just because you have the emotional range of a teaspoon doesn’t mean we all have.",
  "reviewerID": "4E82CF3A24D34",
  "reviewerName": "Emilee Heidenreich",
  "summary": "Debitis at facere minus animi quos sed.",
  "unixReviewTime": 1455120950
}
{
  "asin": "B00000AQ4N",
  "helpful": [
    35,
    57
  ],
  "overall": 2.0,
  "reviewText": "Happiness can be found even in the darkest of times if only one remembers to turn on the light.",
  "reviewerID": "7D04AF18AA084",
  "reviewerName": "Shon Balistreri",
  "summary": "Repellat laborum ab necessitatibus id ut minus repellendus.",
  "unixReviewTime": 1571581258
}
{
  "asin": "B000JQ0JNS",
  "helpful": [
    32,
    87
  ],
  "overall": 4.0,
  "reviewText": "Harry, suffering like this proves you are still a man! This pain is part of being human...the fact that you can feel pain like this is your greatest strength.",
  "reviewerID": "53110BA721544",
  "reviewerName": "Lisa Batz",
  "summary": "Dolorem beatae est ea quidem.",
  "unixReviewTime": 1466668179
}
{
  "asin": "B000KFZ32A",
  "helpful": [
    5,
    19
  ],
  "overall": 3.0,
  "reviewText": "After all this time? Always.",
  "reviewerID": "539457305BE84",
  "reviewerName": "Voncile Heathcote",
  "summary": "Distinctio reiciendis quo amet qui molestiae non.",
  "unixReviewTime": 1404997356
}
{
  "asin": "B00000AQ4N",
  "helpful": [
    67,
    69
  ],
  "overall": 4.0,
  "reviewText": "To the well-organized mind, death is but the next great adventure.",
  "reviewerID": "C7812FD6D0464",
  "reviewerName": "Cinderella Wunsch",
  "summary": "Qui aspernatur facere.",
  "unixReviewTime": 1270258819
}
{
  "asin": "B000NI7RW8",
  "helpful": [
    0,
    24
  ],
  "overall": 4.0,
  "reviewText": "To the well-organized mind, death is but the next great adventure.",
  "reviewerID": "761045EEC00D4",
  "reviewerName": "Luisa Kling",
  "summary": "Et non earum.",
  "unixReviewTime": 1447118407
}
{
  "asin": "B000KFZ32A",
  "helpful": [
    23,
    23
  ],
  "overall": 1.0,
  "reviewText": "Just because you have the emotional range of a teaspoon doesn’t mean we all have.",
  "reviewerID": "E7A5F7E40C8D4",
  "reviewerName": "Micah Robel",
  "summary": "Animi ut minus et consequatur placeat voluptas.",
  "unixReviewTime": 1347189467
}
{
  "asin": "B0002F40AY",
  "helpful": [
    0,
    1
  ],
  "overall": 5.0,
  "reviewText": "We could all have been killed - or worse, expelled.",
  "reviewerID": "FC7F1F6A10354",
  "reviewerName": "Lynelle Robel",
  "summary": "Excepturi quo explicabo et.",
  "unixReviewTime": 1348778489
}
{
  "asin": "B000NI7RW8",
  "helpful": [
    0,
    86
  ],
  "overall": 4.0,
  "reviewText": "It takes a great deal of bravery to stand up to our enemies, but just as much to stand up to our friends.",
  "reviewerID": "1533FADBABEA4",
  "reviewerName": "Wilburn Mohr",
  "summary": "Sapiente aspernatur ut.",
  "unixReviewTime": 1339051628
}
{
  "asin": "B000654P8C",
  "helpful": [
    74,
    75
  ],
  "overall": 3.0,
  "reviewText": "It takes a great deal of bravery to stand up to our enemies, but just as much to stand up to our friends.",
  "reviewerID": "392704CA61D64",
  "reviewerName": "Homer Walter",
  "summary": "Non quisquam tempora rerum veritatis saepe eos.",
  "unixReviewTime": 1305588946
}
{
  "asin": "B0002F40AY",
  "helpful": [
    17,
    27
  ],
  "overall": 2.0,
  "reviewText": "Happiness can be found even in the darkest of times if only one remembers to turn on the light.",
  "reviewerID": "A23670C1E18E4",
  "reviewerName": "Donte Deckow",
  "summary": "Vel necessitatibus cum animi.",
  "unixReviewTime": 1342596834
}
{
  "asin": "B000654P8C",
  "helpful": [
    3,
    76
  ],
  "overall": 2.0,
  "reviewText": "It is our choices, Harry, that show what we truly are, far more than our abilities.",
  "reviewerID": "A35CECDD3AEB4",
  "reviewerName": "Douglass Jacobs",
  "summary": "Possimus quae labore.",
  "unixReviewTime": 1522847344
}
{
  "asin": "B000JQ0JNS",
  "helpful": [
    23,
    54
  ],
  "overall": 5.0,
  "reviewText": "Harry, suffering like this proves you are still a man! This pain is part of being human...the fact that you can feel pain like this is your greatest strength.",
  "reviewerID": "7A2294BB37D54",
  "reviewerName": "Adeline Langosh",
  "summary": "Hic est in occaecati nihil in dolores.",
  "unixReviewTime": 1476369800
}
{
  "asin": "B0002F40AY",
  "helpful": [
    6,
    12
  ],
  "overall": 3.0,
  "reviewText": "We could all have been killed - or worse, expelled.",
  "reviewerID": "CAFC0D7AE9464",
  "reviewerName": "Domenic Cremin",
  "summary": "Qui asperiores ut maxime qui nihil neque.",
  "unixReviewTime": 1543546718
}
```

As for the structure:

- `reviewerID` - ID of the reviewer, e.g. `A2SUAM1J3GNN3B`
- `asin` - ID of the product, e.g. `0000013714`
- `reviewerName` - name of the reviewer (not always specified)
- `helpful` - helpfulness rating of the review, e.g. 5/7 means that there were 7 votes, 5 of which voted this review as
  helpful and 2 of which voted not helpful
- `reviewText` - text of the review
- `overall` - rating of the product (1 to 5 stars)
- `summary` - a brief summary of the review
- `unixReviewTime` - time of the review (unix time)

You can download a larger example
file [here](https://hivemind-share.s3-eu-west-1.amazonaws.com/codingchallenge/resources/amazon-reviews.json.gz).

## The requirements

Your web service is given a path to the file that contains the reviews once when starting up.
It should then be running and listening on `localhost:8080`.

### Find the best rated products

We want to search for best rated products within a certain period of time. The rating of a product is determined by the
average number of stars (the `overall` field). Higher numbers means better rating. A date range **in UTC** (in which to
search) will be given and the number of results should be limited by the given value `limit`. We only want to consider
products that have a minimum number of reviews given by the parameter `min_number_reviews`.

The request will be made like this:

```http
POST /amazon/best-rated HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "start": "01.01.2010",
  "end": "31.12.2020",
  "limit": 2,
  "min_number_reviews": 2
}
```

Your API should respond to such a request by providing a list of results, where each result contains the product
id (`asin`) and the average rating.

When working with the [test data](/resources/video_game_reviews_example.json) a request like the one above should be
answered like that:

```json
[
  {
    "asin": "B000JQ0JNS",
    "average_rating": 4.5
  },
  {
    "asin": "B000NI7RW8",
    "average_rating": 3.666666666666666666666666666666667
  }
]
```

### Additional requirements

- The path to the file should be configurable
- Keep in mind that the file might be too large to fit into memory

## Running the service

We will run your service and see if it does what it is expected to do.
Please make sure to give us some instructions about how we can run it on our machines.
Ideally it should run independent on the platform and not require a big machine.

## Tips

- If you are getting stuck and unable to proceed with the exercise, or if you have any other questions, don't hesitate
  to contact us. Communication is key.
- Please explicitly note any assumptions that you make.
- The choice of libraries and framework is on purpose completely up to you, and these decisions can form a basis of
  discussion for any further interview.
- Keep in mind that we place a high value on purely functional programming and would appreciate your code adhering to
  that. We strongly believe that pure FP solutions lead to more readable code.
- If you plan to choose something else than FP please let us know about your reasoning during your 2nd interview.