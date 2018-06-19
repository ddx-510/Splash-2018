import webhoseio
import nltk
from nltk.tokenize import word_tokenize
from nltk.corpus import stopwords
nltk.download('stopwords')

sw = stopwords.words('english')
sw = sw + ['lah','ah','think','believe','see','want','leh','meh','lor','eh','oi']
agree = ['yes','ya','ok','yeah','sure','okay']
curse = ['fuck','shit','asshole','bitch','dick','cunt','vagina','penis','nigger','bastard','shitty','ass','fucking','fucker','sucker','suck']
webhoseio.config(token="985f40b1-117f-4973-8e56-d1121fb9c5b8")
C = 0 #Article index count, adds one after each successful print.
sorttype = ""
output = 0

def typeSort():#Returns what type of article the user wants                                            
    global sorttype
    typeConfirm = input("Do you want some news or opinions on this?")
    TC = typeConfirm.split()
    for j in TC:
        if j.lower() == "news" or j.lower() == "articles":
            sorttype = "news"
            return sorttype
        elif j.lower() == "opinions" or j.lower() == "opinion":
            sorttype = "blogs"
            return sorttype
        else:
            print("Sorry, can you repeat?")
            typeSort()


def printArticle():#Print the article
    global output
    global C
    print('This is the '+sorttype+' from '+ output['posts'][C]['thread']['site'] + ' published on '+ output['posts'][C]['published'])
    cont = input('Do you want to hear the whole story?')
    conti = cont.split()
    for i in conti:
        if i.lower() in agree:
            print(output['posts'][C]['text'])
            C = C + 1
            break
        else:
            print('Okay, glad to help you!')
            C = C + 1
            break

def main():
    global output
    qn = input('What do you want to ask?')

    tokens = word_tokenize(qn)
    Tokens = []
    for token in tokens:
        if token.lower() not in sw:
            Tokens.append(token)
    qnF = ' '.join(Tokens)

    typeSort()

    query_params = {
        "q": qnF + " language:english site_type:"+ sorttype,
        "ts": "1526543100240",
        "sort": "crawled"
    }

    output = webhoseio.query("filterWebContent", query_params)

    firstPost = []
    if sorttype is "blogs":
        for h in output['posts']:
            if curse in output['posts']:
                continue
            else:
                firstPost.append(h)
                printArticle()
                break
    else:
        printArticle()
        output = webhoseio.get_next()

    again = input("Do you want to hear about something else?")
    for x in again.split():
        if x.lower() in agree:
            main()
        else:
            print("Good day! See you!")
            break

main()
